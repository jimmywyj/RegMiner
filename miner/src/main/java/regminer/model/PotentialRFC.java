package regminer.model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PotentialRFC {
    public Map<String, File> fileMap = new HashMap<>();
    private final RevCommit commit;
    private String buggyCommitId;
    private int priority;
    private List<NormalFile> normalJavaFiles; //which contains fix path
    private List<TestFile> testCaseFiles;    // All File Under test dir
    private final List<TestFile> testRelates = new ArrayList<TestFile>(); //under test dir but not testcase
    private List<SourceFile> sourceFiles = new ArrayList<>(); //config file or data for test


    public PotentialRFC(RevCommit commit) {
        this.commit = commit;
    }

    public RevCommit getCommit() {
        return commit;
    }

    public List<NormalFile> getNormalJavaFiles() {
        return normalJavaFiles;
    }

    public void setNormalJavaFiles(List<NormalFile> normalJavaFiles) {
        this.normalJavaFiles = normalJavaFiles;
    }

    public List<TestFile> getTestCaseFiles() {
        return testCaseFiles;
    }

    public void setTestCaseFiles(List<TestFile> testCaseFiles) {
        this.testCaseFiles = testCaseFiles;
    }


    public void setPriority(int priority) {
        this.priority = priority;
    }


    public List<TestFile> getTestRelates() {
        return testRelates;
    }


    public List<SourceFile> getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(List<SourceFile> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public String getBuggyCommitId() {
        return buggyCommitId;
    }

    public void setBuggyCommitId(String buggyCommitId) {
        this.buggyCommitId = buggyCommitId;
    }

}
