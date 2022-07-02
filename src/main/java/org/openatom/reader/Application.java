package org.openatom.reader;


import com.alibaba.fastjson.JSONObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;
import org.openatom.reader.utils.JGitUtils;
import org.openatom.reader.utils.NoteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author ronin
 * @version V1.0
 * @since 2019/12/4 13:27
 */

public class Application {
    private static Logger log = LoggerFactory.getLogger(Application.class);

    private String remoteGitRepositoryUri;
    private String masterBranchName;
    private String pagesBranchName;
    private String baseGitRepositoryLocation;
    private UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider;
    {
        Yaml yaml = new Yaml();
        JSONObject application = yaml.loadAs(JGitUtils.class.getResourceAsStream("/application.yml"), JSONObject.class);
        JSONObject git = application.getJSONObject("git");
        remoteGitRepositoryUri = git.getJSONObject("uri").get("github").toString();
        masterBranchName = git.getJSONObject("branch").getJSONObject("github").get("master").toString();
        pagesBranchName = git.getJSONObject("branch").getJSONObject("github").get("pages").toString();
        baseGitRepositoryLocation = System.getProperty("user.dir") + "\\src\\main\\resources\\repository";
        JSONObject token = git.getJSONObject("token").getJSONObject("github");
        usernamePasswordCredentialsProvider =
                new UsernamePasswordCredentialsProvider(token.get("username").toString(),token.get("password").toString());
    }

    private String noteFilesLocation;
    private String htmlFilesLocation;
    {
        noteFilesLocation = System.getProperty("user.dir") + "\\notes";
        Yaml yaml = new Yaml();
        JSONObject application = yaml.loadAs(NoteUtils.class.getResourceAsStream("/application.yml"), JSONObject.class);
        JSONObject git = application.getJSONObject("git");
        htmlFilesLocation = git.getJSONObject("htmlFilesLocation").getJSONObject("github").get("pages").toString();
    }

    @Test
    public void testCompositeCommand() throws IOException, GitAPIException, URISyntaxException {
        JGitUtils jGitUtils = new JGitUtils();
        log.info("开始执行git操作:----------------------------------");
        //String operatorBranchName = masterBranchName;
        String operatorBranchName = pagesBranchName;
        String dotGitLocation = baseGitRepositoryLocation + "\\" + operatorBranchName + "\\.git";
        Git git = new Git(new FileRepository(dotGitLocation));
        //下载代码到本地
        jGitUtils.downloadFromRemoteRepository(git,baseGitRepositoryLocation,
                usernamePasswordCredentialsProvider,operatorBranchName,remoteGitRepositoryUri);

        NoteUtils noteUtils = new NoteUtils();
        noteUtils.convertAllTxtNotesToHtml(noteFilesLocation,htmlFilesLocation);
        //上传代码到远程仓库
        String gitCommitMessage ="请回答3306";
        jGitUtils.uploadToRemoteRepository(git,baseGitRepositoryLocation,
                gitCommitMessage,usernamePasswordCredentialsProvider,operatorBranchName);
        log.info("结束执行git操作:----------------------------------");
    }

    //先把page分支拉下来
    //然后把note放在page分支中
    //提交page分支
}
