package regminer.miner.migrate;

import org.jetbrains.annotations.NotNull;
import regminer.constant.Conf;
import regminer.exec.TestExecutor;
import regminer.finalize.SycFileCleanup;
import regminer.model.*;
import regminer.utils.FileUtilx;

import java.io.File;
import java.util.*;

public class BICFinder {
    private final static String dataPath = "results/fix_and_introducers_pairs.json";
    final int level = 0;
    TestExecutor exec = new TestExecutor();
    TestCaseMigrator testMigrater = new TestCaseMigrator();
    PotentialRFC pRFC;
    int[] status; // 切勿直接访问该数组
    int passPoint = Integer.MIN_VALUE;
    int falPoint = Integer.MAX_VALUE;

    public BICFinder() {
    }

    // ================================
    // SZZ BLOCK CODE 方法目前只在linux上使用
    // ================================
    public Set<String> getBICSet() {
        createBICLog();
        return readBICSetFromFile();
    }

    public void createBICLog() {
        exec.exec("rm -rf issues");
        exec.exec("rm -rf results");
        exec.exec(
                "java -jar szz_find_bug_introducers-0.1.jar -i issue_list.json -r /home/sxz/Documents/meta/fastjson -d 3 -c 1");
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {

        }
    }

    public Set<String> readBICSetFromFile() {
        String originData = FileUtilx.readContentFromFile(dataPath);
        originData = originData.replace("[", "").replace("]", "").replace("\"", "").replace("\n", "");
        String[] originDataArray = originData.split(",");
        Set<String> bICSet = new HashSet<>();
        for (int i = 0; i < originDataArray.length; i++) {
            bICSet.add(originDataArray[i]);
            FileUtilx.log(originDataArray[i]);
        }
        bICSet.remove(originDataArray[0]);
        return bICSet;
    }

    // ==========================
    // SZZ BLOCK CODE END
    // ==============================

    /**
     * @param pRFC
     * @return
     */
    public Regression searchBIC(@NotNull PotentialRFC pRFC) {
        FileUtilx.log(pRFC.getCommit().getName() + " Start search");
        passPoint = Integer.MIN_VALUE;

        // 预防方法被错误的调用
        // 情况1 search时候bfc没有确定的测试用例
        List<TestFile> testSuites = pRFC.getTestCaseFiles();
        if (testSuites == null || testSuites.size() == 0) {
            FileUtilx.log("意外的错误：NOSET");
            return null;
        }
        // 情况2 BFC没有parent，没必要search
        if (pRFC.getCommit().getParentCount() <= 0) {
            FileUtilx.log("searchBIC no Parent");
            return null;
        }
        // 方法主要逻辑
        this.pRFC = pRFC;
        // 获取BFC到Origin的所有CommitID
        List<String> candidateList = revListCommand(pRFC.getCommit().getParent(0).getName());
        // 得到反转数组,即从Origin到Commit
        Collections.reverse(candidateList);
        String[] arr = candidateList.toArray(new String[candidateList.size()]);
        falPoint = arr.length - 1;
        // 针对每一个BFC使用一个status数组记录状态，测试过的不再测试
        status = new int[arr.length];
        for (int i = 0; i < status.length; i++) {
            status[i] = -2000;
        }
        // recursionBinarySearch(arr, 1, arr.length - 1);//乐观二分查找，只要不能编译，就往最新的时间走
        int a = search(arr, 1, arr.length - 1); // 指数跳跃二分查找
        // 处理search结果
        String bfcId = pRFC.getCommit().getName();
        File bfcFile = new File(Conf.CACHE_PATH + File.separator + bfcId);

        //have pass but not hit regression
        if (a < 0 && passPoint >= 0) {
            if (passPoint > falPoint) {
                falPoint = arr.length - 1;
            }
            if (passPoint < falPoint) {
                FileUtilx.log("start searchStepByStep");
                searchStepByStep(arr);
            }
            if (passPoint == falPoint) {
                FileUtilx.log("Excepted! here passPoint eq falPoint");
                System.out.println("Excepted! here passPoint eq falPoint");
            }
        }

        //handle hit result
        if (a >= 0 || (falPoint - passPoint) == 1) {
            String working="";
            String bic ="";
            if (a >= 0) {
                working = arr[a];
                bic = arr[a+1];
            }else if (a<0 && passPoint>=0 && (falPoint-passPoint)==1){
                working = arr[passPoint];
                bic =arr[falPoint];
            }
            if (working==""&&bic==""){
                FileUtilx.log("work and bic eq empty");
                System.out.println("work and bic eq empty");
                return  null;
            }
            FileUtilx.log("regression+1");
            // 如果是regression组合一下需要记录的相关数据
            // 顺便恢复一下exec的目录，防止exec正在的目录已被删除
            exec.setDirectory(new File(Conf.PROJECT_PATH));
            String testcaseString = combinedRegressionTestResult();
            String bfcpId = pRFC.getBuggyCommitId();
            // 删除bfc目录下的其他构建测试历史文件
            new SycFileCleanup().cleanDirectoryOnFilter(bfcFile, Arrays.asList(bfcId, bfcpId, bic,working));// 删除在regression定义以外的项目文件
            return new Regression(Conf.PROJRCT_NAME + "_" + bfcId, bfcId, bfcpId, bic, working,
                    pRFC.fileMap.get(bfcId).getPath(), pRFC.fileMap.get(bfcpId).getPath(), pRFC.fileMap.get(bic).getPath(), pRFC.fileMap.get(working).getPath(), testcaseString);
        }else if(a<0 && passPoint>=0 &&(falPoint-passPoint)>1){
            FileUtilx.log("regression+1,with gap");
            exec.setDirectory(new File(Conf.PROJECT_PATH));
            String testcaseString = combinedRegressionTestResult();
            String bfcpId = pRFC.getBuggyCommitId();
            // 删除bfc目录下的其他构建测试历史文件
            new SycFileCleanup().cleanDirectoryOnFilter(bfcFile, Arrays.asList(bfcId, bfcpId, arr[falPoint], arr[passPoint]));// 删除在regression定义以外的项目文件
            return new RegressionWithGap(Conf.PROJRCT_NAME + "_" + bfcId, bfcId, bfcpId, arr[falPoint], arr[passPoint],
                    pRFC.fileMap.get(bfcId).getPath(), pRFC.fileMap.get(bfcpId).getPath(), pRFC.fileMap.get(arr[falPoint]).getPath(), pRFC.fileMap.get(arr[passPoint]).getPath(), testcaseString);
        }
        exec.setDirectory(new File(Conf.PROJECT_PATH));
        new SycFileCleanup().cleanDirectory(bfcFile);
        return null;
    }

    public void searchStepByStep(String[] arr) {
        int now = passPoint + 1;
        int i = 0;
        while (now <= falPoint && i < 50) {
            ++i;
            int a = getTestResult(arr[now], now);
            if (a == TestCaseMigrator.PASS) {
                passPoint = now;
            } else if (a == TestCaseMigrator.FAL) {
                falPoint = now;
                return;
            }
            ++now;
        }
        now = falPoint - 1;
        i = 0;
        while (now >= passPoint && i < 50) {
            ++i;
            int a = getTestResult(arr[now], now);
            if (a == TestCaseMigrator.PASS) {
                passPoint = now;
                return;
            } else if (a == TestCaseMigrator.FAL) {
                falPoint = now;
            }
            --now;
        }
    }

    /**
     * 此方法组合regression的最终测试用力文本
     *
     * @return
     */
    public String combinedRegressionTestResult() {
        StringJoiner sj = new StringJoiner(";", "", "");
        for (TestFile tc : pRFC.getTestCaseFiles()) {
            Map<String, RelatedTestCase> methodMap = tc.getTestMethodMap();
            if (methodMap == null) {
                continue;
            }
            for (Iterator<Map.Entry<String, RelatedTestCase>> it = methodMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, RelatedTestCase> entry = it.next();
                String testCase = tc.getQualityClassName() + Conf.methodClassLinkSymbolForTest
                        + entry.getKey().split("[(]")[0];
                sj.add(testCase);
            }
        }
        return sj.toString();
    }

    public int getTestResult(String bic, int index) {
        if (status[index] != -2000) {
            return status[index];
        } else {
            return test(bic, index);
        }
    }

    public int test(String bic, int index) {
        try {
            int result = testMigrater.migrate(pRFC, bic);
            status[index] = result;
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1000;
    }

    /**
     * 乐观二分查找，现在已经放弃使用
     *
     * @param arr
     * @param low
     * @param high
     * @return
     */
    public int recursionBinarySearch(String[] arr, int low, int high) {

        if (low > high) {
            FileUtilx.log("search fal");
            return -1;
        }

        int middle = (low + high) / 2; // 初始中间位置

        int a = test(arr[middle], middle);
        boolean result = a == TestCaseMigrator.FAL;
        int b = test(arr[middle - 1], middle);
        boolean result1 = b == TestCaseMigrator.PASS;
        if (result && result1) {
            FileUtilx.log("regression+1");
            return middle;
        }
        if (result) {
            // 测试用例不通过往左走
            return recursionBinarySearch(arr, low, middle - 1);

        } else {
            return recursionBinarySearch(arr, middle + 1, high);
        }
    }

    /**
     * arr数组中的元素是bfc执行rev-list所得到的commitID数组
     *
     * @param arr
     * @param low
     * @param high
     * @return if find regression return working index
     */
    public int search(String[] arr, int low, int high) {
        // 失败条件
        if (low > high || low < 0 || high > arr.length - 1) {
            FileUtilx.log("search fal");
            return -1;
        }

        int middle = (low + high) / 2;
        // 查找成功条件
        int statu = getTestResult(arr[middle], middle);


        if (statu == TestCaseMigrator.FAL && middle - 1 >= 0
                && getTestResult(arr[middle - 1], middle - 1) == TestCaseMigrator.PASS) {
            return middle - 1;
        }
        if (statu == TestCaseMigrator.PASS && middle + 1 < arr.length
                && getTestResult(arr[middle + 1], middle + 1) == TestCaseMigrator.FAL) {
            return middle;
        }
        if (statu == TestCaseMigrator.FAL && middle < falPoint) {
            falPoint = middle;
        }
        if (statu == TestCaseMigrator.PASS && middle > passPoint) {
            passPoint = middle;
        }
        // 查找策略
        if (statu == TestCaseMigrator.CE) {
            // 指数跳跃查找
            int left = expLeftBoundry(arr, low, middle, 0);

            if (left != -1 && getTestResult(arr[left], left) == TestCaseMigrator.FAL) {
                // 往附近看一眼
                if (middle - 1 >= 0 && getTestResult(arr[left - 1], left - 1) == TestCaseMigrator.PASS) {
                    return left - 1;
                }
                // 左边界开始新的查找
                int a = search(arr, low, left);
                if (a != -1) {
                    return a;
                }
            }
            int right = expRightBoundry(arr, middle, high, 0);

            if (right != -1 && getTestResult(arr[right], right) == TestCaseMigrator.PASS) {
                // 往附近看一眼
                if (middle + 1 < arr.length && getTestResult(arr[right + 1], right + 1) == TestCaseMigrator.FAL) {
                    return right;
                }
                int b = search(arr, right, high);
                if (b != -1) {
                    return b;
                }
            }
            FileUtilx.log("search fal");
            return -1;
        } else if (statu == TestCaseMigrator.FAL) {
            // notest 等unresolved的情况都乐观的往右
            return search(arr, low, middle - 1);// 向左
        } else {
            return search(arr, middle + 1, high); // 向右
        }
    }

    public int expLeftBoundry(String[] arr, int low, int high, int index) {
        int left = high;
        int status = 0;
        int pos = -1;
        for (int i = 0; i < 18; i++) {
            if (left < low) {
                return -1;
            } else {
                pos = left - (int) Math.pow(2, i);
                if (pos < low) {
                    if (index < level) {
                        return expLeftBoundry(arr, low, left, index + 1);
                    } else {
                        return -1;
                    }
                }
                left = pos;
                status = getTestResult(arr[left], left);
                if (status != -1) {
                    return rightTry(arr, left, high);
                }
            }

        }
        return -1;
    }

    public int rightTry(String[] arr, int low, int high) {
        int right = low;
        int status = 0;
        int pos = -1;
        for (int i = 0; i < 18; i++) {
            if (right > high) {
                return right;
            } else {
                pos = right + (int) Math.pow(2, i);
                if (pos > high) {
                    return right;
                }
                status = getTestResult(arr[pos], pos);
                if (status == -1) {
                    return right;
                } else {
                    right = pos;
                }
            }
        }
        return right;
    }

    public int leftTry(String[] arr, int low, int high) {
        int left = high;
        int status = 0;
        int pos = -1;
        for (int i = 0; i < 18; i++) {
            if (left < low) {
                return left;
            } else {
                pos = left - (int) Math.pow(2, i);
                if (pos < low) {
                    return left;
                }
                status = getTestResult(arr[pos], pos);
                if (status == -1) {
                    return left;
                } else {
                    left = pos;
                }
            }
        }
        return left;
    }

    public int expRightBoundry(String[] arr, int low, int high, int index) {
        int right = low;
        int status = 0;
        int pos = -1;
        for (int i = 0; i < 18; i++) {
            if (right > high) {
                return -1;
            } else {
                pos = right + (int) Math.pow(2, i);
                if (pos > high) {
                    if (index < level) {
                        return expRightBoundry(arr, right, high, index + 1);
                    } else {
                        return -1;
                    }
                }
                right = pos;
                status = getTestResult(arr[right], right);
                if (status != -1) {
                    return leftTry(arr, low, right);
                }
            }
        }
        return -1;
    }

    public List<String> revListCommand(String commitId) {
        exec.setDirectory(new File(Conf.META_PATH));
        exec.runCommand("git checkout -f master");
        return exec.runCommand("git rev-list " + commitId);
    }
}
