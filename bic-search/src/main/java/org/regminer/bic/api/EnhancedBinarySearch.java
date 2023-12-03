package org.regminer.bic.api;

import org.apache.commons.lang3.tuple.Triple;
import org.regminer.bic.api.core.BICSearchStrategy;
import org.regminer.common.constant.Configurations;
import org.regminer.common.model.PotentialBFC;
import org.regminer.common.tool.SycFileCleanup;
import org.regminer.common.utils.FileUtilx;
import org.regminer.common.utils.GitUtils;
import org.regminer.ct.api.CtContext;
import org.regminer.ct.model.TestCaseResult;
import org.regminer.ct.model.TestResult;
import org.regminer.migrate.api.TestCaseMigrator;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @Author: sxz
 * @Date: 2022/06/09/00:11
 * @Description:
 */
public class EnhancedBinarySearch extends BICSearchStrategy {
    final int level = 0;
    protected Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
    TestCaseResult.TestState[] status; // 切勿直接访问该数组
    //XXX:CompileErrorSearch block
    //all related code need involve
    int passPoint = Integer.MIN_VALUE;
    int falPoint = Integer.MAX_VALUE;
    private TestCaseMigrator testMigrator;
    private CtContext ctContext;
    private PotentialBFC pRFC;

    public void setTestMigrator(TestCaseMigrator testCaseMigrator) {
        this.testMigrator = testCaseMigrator;
    }

    public void setProjectBuilder(CtContext ctContext) {
        this.ctContext = ctContext;
    }

    @Override
    public String[] getSearchSpace(String startPoint, File coeDir) {
        // 得到反转数组,即从Origin到Commit
        List<String> candidateList = GitUtils.revListCommand(startPoint, coeDir);
        Collections.reverse(candidateList);//TODO Song Xuezhi 测试确定是不是从历史到现在
        return new String[0];
    }

    //该算法实际上是将git bisect的功能阉割实现了一遍，gitbisect实际上会考虑图的拓扑结构
    //这里我的考虑方式是使用拓扑排序，将提交历史变成线性结构。这是一种退而求其次的方式，但保证了真实的父子关系。
    @Override
    public Triple<String, String, Integer> search(PotentialBFC potentialBFC) {
        this.pRFC = potentialBFC;
        String bfcId = pRFC.getCommit().getName();
        File bfcFile = new File(Configurations.cachePath + File.separator + bfcId);
        try {
            logger.info(pRFC.getCommit().getName() + " Start search");
            passPoint = Integer.MIN_VALUE;
            // 方法主要逻辑
            String[] arr = getSearchSpace(pRFC.getCommit().getParent(0).getName(), pRFC.fileMap.get(bfcId));
            falPoint = arr.length - 1;
            // 针对每一个BFC使用一个status数组记录状态，测试过的不再测试
            status = new TestCaseResult.TestState[arr.length];
            for (int i = 0; i < status.length; i++) {
                status[i] = TestCaseResult.TestState.NOMARK;
            }
            // recursionBinarySearch(arr, 1, arr.length - 1);//乐观二分查找，只要不能编译，就往最新的时间走
            int a = search(arr, 1, arr.length - 1); // 指数跳跃二分查找 XXX:CompileErrorSearch

            // 处理search结果
            //have pass but not hit regression
            if (a < 0 && passPoint >= 0) {
                if (passPoint > falPoint) {
                    falPoint = arr.length - 1;
                }
                if (passPoint < falPoint) {
                    logger.info("start searchStepByStep");
                    searchStepByStep(arr);
                }
                if (passPoint == falPoint) {
                    logger.info("Excepted! here passPoint eq falPoint");
                }
            }

            //handle hit result
            if (a >= 0 || (falPoint - passPoint) == 1) {
                String working = "";
                String bic = "";
                if (a >= 0) {
                    working = arr[a];
                    bic = arr[a + 1];
                } else if (passPoint >= 0 && falPoint - passPoint == 1) {
                    working = arr[passPoint];
                    bic = arr[falPoint];
                }
                if (working.isEmpty() && bic.isEmpty()) {
                    logger.error("work and bic eq empty");
                    return null;
                }

                new SycFileCleanup().cleanDirectory(bfcFile);
                return Triple.of(working, bic, 1);
            } else if (passPoint >= 0 && falPoint - passPoint > 1) {
                logger.info("regression+1,with gap");
                return Triple.of(arr[passPoint], arr[falPoint], falPoint - passPoint);
            }
            return null;
        } finally {

            new SycFileCleanup().cleanDirectory(bfcFile);// 删除在regression定义以外的项目文件
        }
    }

    //该算法实际上是将git bisect的功能阉割实现了一遍，gitbisect实际上会考虑图的拓扑结构
    //这里我的考虑方式是使用拓扑排序，将提交历史变成线性结构。这是一种退而求其次的方式，但保证了真实的父子关系。

    public void searchStepByStep(String[] arr) {
        int now = passPoint + 1;
        int i = 0;
        TestCaseResult.TestState result = TestCaseResult.TestState.UNKNOWN;
        while (now <= falPoint && i < 2) {
            ++i;
            result = getTestResult(arr[now], now);
            if (result == TestCaseResult.TestState.PASS) {
                passPoint = now;
            } else if (result == TestCaseResult.TestState.FAL) {
                falPoint = now;
                return;
            }
            ++now;
        }
        now = falPoint - 1;
        i = 0;
        while (now >= passPoint && i < 2) {
            ++i;
            result = getTestResult(arr[now], now);
            if (result == TestCaseResult.TestState.PASS) {
                passPoint = now;
                return;
            } else if (result == TestCaseResult.TestState.FAL) {
                falPoint = now;
            }
            --now;
        }
    }


    public TestCaseResult.TestState getTestResult(String bic, int index) {
        logger.info("index:" + index + ":" + bic);
        TestCaseResult.TestState result;
        if (status[index] != TestCaseResult.TestState.NOMARK) {
            result = status[index];
        } else {
            result = test(bic, index);
        }
        if (result == TestCaseResult.TestState.FAL && index < falPoint) {
            falPoint = index;
        }
        if (result == TestCaseResult.TestState.PASS && index > passPoint) {
            passPoint = index;
        }
        logger.info(result.name().toString());
        return result;
    }


    public TestCaseResult.TestState test(String bic, int index) {
        try {
            TestResult testResult = testMigrator.migrate(pRFC, bic);
            status[index] = testResult.getCaseResultMap().values().contains(TestCaseResult.TestState.PASS) ?
                    TestCaseResult.TestState.PASS :
                    TestCaseResult.TestState.FAL;
            return status[index];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return TestCaseResult.TestState.UNKNOWN;
    }


//    // XXX:git bisect
//    public int gitBisect(String[] arr, int low, int high) {
//
//        if (low > high) {
//            FileUtilx.log("search fal");
//            return -1;
//        }
//
//        int middle = (low + high) / 2; // 初始中间位置
//
//        int a = test(arr[middle], middle);
//        boolean result = a == TestCaseResult.TestState.FAL;
//
//        if (a == TestCaseResult.TestState.CE || a == TestCaseResult.TestState.UNKNOWN) {
//            return -1;
//        }
//        int b = test(arr[middle - 1], middle);
//        boolean result1 = b == TestCaseResult.TestState.PASS;
//        if (b == TestCaseResult.TestState.CE || b == TestCaseResult.TestState.UNKNOWN) {
//            return -1;
//        }
//        if (result && result1) {
//            FileUtilx.log("regression+1");
//            return middle;
//        }
//        if (result) {
//            // 测试用例不通过往左走
//            return gitBisect(arr, low, middle - 1);
//
//        } else if (result1) {
//            return gitBisect(arr, middle + 1, high);
//        } else {
//            return -1;
//        }
//    }

    /**
     * 乐观二分查找，现在已经放弃使用
     * XXX:Optimism Search
     *
     * @param arr
     * @param low
     * @param high
     * @return
     */
//    public int recursionBinarySearch(String[] arr, int low, int high) {
//
//        if (low > high) {
//            FileUtilx.log("search fal");
//            return -1;
//        }
//
//        int middle = (low + high) / 2; // 初始中间位置
//
//        int a = test(arr[middle], middle);
//        boolean result = a == TestCaseResult.TestState.FAL;
//        int b = test(arr[middle - 1], middle);
//        boolean result1 = b == TestCaseResult.TestState.PASS;
//        if (result && result1) {
//            FileUtilx.log("regression+1");
//            return middle;
//        }
//        if (result) {
//            // 测试用例不通过往左走
//            return recursionBinarySearch(arr, low, middle - 1);
//
//        } else {
//            return recursionBinarySearch(arr, middle + 1, high);
//        }
//    }


    /**
     * arr数组中的元素是bfc执行rev-list所得到的commitID数组
     * XXX:CompileErrorSearch
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
        TestCaseResult.TestState result = getTestResult(arr[middle], middle);

        if (result == TestCaseResult.TestState.FAL && middle - 1 >= 0
                && getTestResult(arr[middle - 1], middle - 1) == TestCaseResult.TestState.PASS) {
            return middle - 1;
        }
        if (result == TestCaseResult.TestState.PASS && middle + 1 < arr.length
                && getTestResult(arr[middle + 1], middle + 1) == TestCaseResult.TestState.FAL) {
            return middle;
        }
        // 查找策略
        if (result == TestCaseResult.TestState.CE) {
            // 指数跳跃查找
            int left = expLeftBoundary(arr, low, middle, 0);

            if (left != -1 && getTestResult(arr[left], left) == TestCaseResult.TestState.FAL) {
                // 往附近看一眼
                if (middle - 1 >= 0 && getTestResult(arr[left - 1], left - 1) == TestCaseResult.TestState.PASS) {
                    return left - 1;
                }
                // 左边界开始新的查找
                int a = search(arr, low, left);
                if (a != -1) {
                    return a;
                }
            }
            int right = expRightBoundary(arr, middle, high, 0);

            if (right != -1 && getTestResult(arr[right], right) == TestCaseResult.TestState.PASS) {
                // 往附近看一眼
                if (middle + 1 < arr.length && getTestResult(arr[right + 1], right + 1) == TestCaseResult.TestState.FAL) {
                    return right;
                }
                int b = search(arr, right, high);
                if (b != -1) {
                    return b;
                }
            }
            FileUtilx.log("search fal");
            return -1;
        } else if (result == TestCaseResult.TestState.FAL) {
            // notest 等unresolved的情况都乐观的往右
            return search(arr, low, middle - 1);// 向左
        } else {
            return search(arr, middle + 1, high); // 向右
        }
    }

    public int expLeftBoundary(String[] arr, int low, int high, int index) {
        int left = high;
        TestCaseResult.TestState status = null;
        int pos = -1;
        for (int i = 0; i < 18; i++) {
            if (left < low) {
                return -1;
            } else {
                pos = left - (int) Math.pow(2, i);
                if (pos < low) {
                    if (index < level) {
                        return expLeftBoundary(arr, low, left, index + 1);
                    } else {
                        return -1;
                    }
                }
                left = pos;
                status = getTestResult(arr[left], left);
                if (status != TestCaseResult.TestState.CE) {
                    return rightTry(arr, left, high);
                }
            }

        }
        return -1;
    }

    public int rightTry(String[] arr, int low, int high) {
        int right = low;
        TestCaseResult.TestState status = null;
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
                if (status == TestCaseResult.TestState.CE) {
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
        TestCaseResult.TestState status = null;
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
                if (status == TestCaseResult.TestState.CE) {
                    return left;
                } else {
                    left = pos;
                }
            }
        }
        return left;
    }

    public int expRightBoundary(String[] arr, int low, int high, int index) {
        int right = low;
        TestCaseResult.TestState status = null;
        int pos = -1;
        for (int i = 0; i < 18; i++) {
            if (right > high) {
                return -1;
            } else {
                pos = right + (int) Math.pow(2, i);
                if (pos > high) {
                    if (index < level) {
                        return expRightBoundary(arr, right, high, index + 1);
                    } else {
                        return -1;
                    }
                }
                right = pos;
                status = getTestResult(arr[right], right);
                if (status != TestCaseResult.TestState.CE) {
                    return leftTry(arr, low, right);
                }
            }
        }
        return -1;
    }

}
