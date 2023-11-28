package org.regminer.miner.migrate;

import org.apache.commons.io.FileUtils;
import org.regminer.miner.constant.Configurations;
import org.regminer.miner.constant.Constant;
import org.regminer.miner.exec.TestExecutor;
import org.regminer.miner.migrate.model.MergeTask;
import org.regminer.miner.model.ChangedFile;
import org.regminer.miner.model.PotentialBFC;
import org.regminer.miner.model.SourceFile;
import org.regminer.miner.model.TestFile;
import org.regminer.miner.utils.FileUtilx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Migrator {
    TestExecutor exec = new TestExecutor();

    public File checkout(String bfcId, String commitId, String version) throws IOException {
        synchronized (Configurations.META_PATH) {
            String cacheFile = Configurations.CACHE_PATH + File.separator + bfcId + File.separator + commitId + File.separator
                    + version + "_" + UUID.randomUUID();
            File file = new File(cacheFile);
            if (!file.exists()) {
                file.mkdirs();
            }
            exec.setDirectory(file);
            FileUtils.copyDirectoryToDirectory(new File(Configurations.META_PATH), new File(cacheFile));
            File result = new File(cacheFile + File.separator + "meta");
            exec.setDirectory(result);
            exec.execPrintln("git checkout -f " + commitId);
            return result;
        }
    }
    /**
     * @param pRFC
     * @param tDir
     */
    public void mergeTwoVersion_BaseLine(PotentialBFC pRFC, File tDir) {
        /**
         *
         * 注意！bfc的patch中可能存在 普通java文件，测试文件（相关测试用例，非测试用例但测试目录下的java文件），配置文件(测试目录下的，其他)
         * 在base_line中我们只迁移 测试文件，和之前不存在的配置文件（暂时不做文本的merge）
         */
        // 相关测试用例
        List<TestFile> testSuite = pRFC.getTestCaseFiles();
        // 非测试用例的在测试目录下的其他文件

        //###XXX:TestDenpendency BlocK
        List<TestFile> underTestDirJavaFiles = pRFC.getTestRelates();
        List<SourceFile> sourceFiles = pRFC.getSourceFiles();
        //##block end

        // merge测试文件
        // 整合任务
        MergeTask mergeJavaFileTask = new MergeTask();
        mergeJavaFileTask.addAll(testSuite).addAll(underTestDirJavaFiles).addAll(sourceFiles).compute();//XXX:TestDenpendency BlocK
        File bfcDir = pRFC.fileMap.get(pRFC.getCommit().getName());
        for (Map.Entry<String, ChangedFile> entry : mergeJavaFileTask.getMap().entrySet()) {
            String newPathInBfc = entry.getKey();
            if (newPathInBfc.contains(Constant.NONE_PATH)) {
                continue;
            }
            File bfcFile = new File(bfcDir, newPathInBfc);
            File tFile = new File(tDir, newPathInBfc);
            if (tFile.exists()) {
                tFile.deleteOnExit();
            }
            // 直接copy过去
            try {
                FileUtils.forceMkdirParent(tFile);
                FileUtils.copyFileToDirectory(bfcFile, tFile.getParentFile());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

    }

    /**
     *
     * @param pRFC
     * @param targetProjectDirectory
     * @throws IOException
     */
    public void copyToTarget(PotentialBFC pRFC, File targetProjectDirectory) throws IOException {
        // copy
        String targetPath = null;
        File bfcFile = pRFC.fileMap.get(pRFC.getCommit().getName());
        List<ChangedFile> taskFiles = new ArrayList<>();
        //now none test file be remove
        //test Related file is removed after test bfc
        taskFiles.addAll(pRFC.getTestCaseFiles());

        //###XXX:TestDenpendency BlocK
        taskFiles.addAll(pRFC.getTestRelates());
        taskFiles.addAll(pRFC.getSourceFiles());
        //####Block end#####

        for (ChangedFile cFile : taskFiles) {
            File file = new File(bfcFile, cFile.getNewPath());
            // 测试文件是被删除则什么也不作。
            if (cFile.getNewPath().contains(Constant.NONE_PATH)) {
                continue;
            }
            targetPath = cFile.getNewPath();
            // 测试文件不是删除，则copy
            targetPath = FileUtilx.getDirectoryFromPath(targetPath);
            File file1 = new File(targetProjectDirectory, targetPath);
            if (!file1.exists()) {
                file1.mkdirs();
            }
            FileUtils.copyFileToDirectory(file, file1);
        }
    }

}
