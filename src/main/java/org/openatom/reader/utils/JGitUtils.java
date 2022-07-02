package org.openatom.reader.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

//今天我遇到了同样的question.the关键是你的账号和password.if你使用的是你的git服务。您需要在安全中心创建个人token，使用token名称作为账号，使用token作为密码。
public class JGitUtils {
    private static Logger log = LoggerFactory.getLogger(JGitUtils.class);

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

    /**
     * 测试组合在一起的git命令
     * @throws IOException
     * @throws GitAPIException
     * @throws URISyntaxException
     */
    @Test
    public void testCompositeCommand() throws IOException, GitAPIException, URISyntaxException {
        log.info("开始执行git操作:----------------------------------");
        String operatorBranchName = masterBranchName;
        //String operatorBranchName = pagesBranchName;
        String dotGitLocation = baseGitRepositoryLocation + "\\" + operatorBranchName + "\\.git";
        Git git = new Git(new FileRepository(dotGitLocation));
        downloadFromRemoteRepository(git,baseGitRepositoryLocation,
                usernamePasswordCredentialsProvider,operatorBranchName,remoteGitRepositoryUri);
        String gitCommitMessage ="请回答1998999";
        uploadToRemoteRepository(git,baseGitRepositoryLocation,
                gitCommitMessage,usernamePasswordCredentialsProvider,operatorBranchName);
        log.info("结束执行git操作:----------------------------------");
    }

    /**
     * 测试独立的git命令
     * @throws IOException
     * @throws GitAPIException
     * @throws URISyntaxException
     */
    @Test
    public void testEachCommand() throws IOException, GitAPIException, URISyntaxException {
        String operatorBranchName = masterBranchName;
//        String operatorBranchName = pagesBranchName;
        String dotGitLocation = baseGitRepositoryLocation + "\\" + operatorBranchName + "\\.git";
        Git git = new Git(new FileRepository(dotGitLocation));
        this.deleteRepository(baseGitRepositoryLocation);
        this.gitCloneCommand(usernamePasswordCredentialsProvider,
                baseGitRepositoryLocation,remoteGitRepositoryUri,operatorBranchName);
        String newFileName = "请回答1998.txt";
        this.gitAddCommand(git,newFileName,baseGitRepositoryLocation,operatorBranchName);
        String gtCommitMessage = "请回答1998";
        this.gitCommitCommand(git,gtCommitMessage);
        this.gitPushCommand(git,usernamePasswordCredentialsProvider,operatorBranchName);
    }

    /**
     * 操作之前先删除本地删除git仓库
     * @param gitRepositoryLocation 本地仓库位置
     * @throws IOException
     */
    public void deleteRepository(String gitRepositoryLocation) throws IOException {
        log.info("当前操作:开始删除已有本地仓库,仓库位置:" + gitRepositoryLocation);
        File repository = new File(gitRepositoryLocation);
        if(repository.exists()){
            FileUtils.forceDelete(repository);
        }
        log.info("当前操作:完成删除已有本地仓库");
    }

    /**
     * git clone command
     * @param usernamePasswordCredentialsProvider 用户信息
     * @param baseGitRepositoryLocation 本地仓库位置
     * @param remoteGitRepositoryUri 远程仓库地址
     * @param operatorBranchName 操作分支名称
     * @throws IOException
     * @throws GitAPIException
     */
    public void gitCloneCommand(CredentialsProvider usernamePasswordCredentialsProvider,
                                String baseGitRepositoryLocation,String remoteGitRepositoryUri,String operatorBranchName
        ) throws IOException, GitAPIException {
        log.info("当前操作:开始执行:git clone " + remoteGitRepositoryUri);
        //当前操作分支仓库位置
        String gitRepositoryLocation = baseGitRepositoryLocation + "\\" +operatorBranchName;
        log.info("本地仓库存放位置:" + gitRepositoryLocation);
        log.info("操作分支名称:" + operatorBranchName);
        //克隆代码库命令
        CloneCommand cloneCommand = Git.cloneRepository();
        //设置远程URI
        cloneCommand.setURI(remoteGitRepositoryUri);
        //设置clone下来的分支
        cloneCommand.setBranch(operatorBranchName);
        //设置当前项目存放路径
        cloneCommand.setDirectory(new File(gitRepositoryLocation));
        //设置远程服务器上的用户名和密码
        cloneCommand.setCredentialsProvider(usernamePasswordCredentialsProvider);
        Git git = cloneCommand.call();
        //关闭源，以释放本地仓库锁
        git.close();
        log.info("当前操作:完成执行:git clone " + remoteGitRepositoryUri);
    }

    /**
     * git init command
     * @param baseGitRepositoryLocation 本地仓库位置
     * @param operatorBranchName 操作分支名称
     * @throws IOException
     */
    public void gitInitCommand(String baseGitRepositoryLocation,String operatorBranchName) throws IOException {
        log.info("当前操作:开始执行:git init");
        //当前操作分支本地仓库位置
        String dotGitLocation = baseGitRepositoryLocation + operatorBranchName + "\\.git";
        Repository repository = FileRepositoryBuilder.create(new File(dotGitLocation));
        repository.create();
        log.info("当前操作:完成执行:git init");
    }

    /**
     *
     * git add command
     * @param git Git对象
     * @param newFileName 新创建的文件的名称
     * @param baseGitRepositoryLocation 本地仓库位置
     * @param operatorBranchName 当前操作分支名称
     * @throws IOException
     * @throws GitAPIException
     */
    public void gitAddCommand(Git git,String newFileName,String baseGitRepositoryLocation,String operatorBranchName) throws IOException, GitAPIException {
        log.info("当前操作:开始执行:git add " + newFileName);
        //当前操作分支仓库位置
        String gitRepositoryLocation = baseGitRepositoryLocation + "\\" + operatorBranchName;
        String newFileLocation = gitRepositoryLocation +"\\" + newFileName;
        File file = new File(newFileLocation);
        log.info("当前添加文件所在路径:" + file.getPath());
        file.createNewFile();
        //添加文件
        git.add().addFilepattern(newFileName).call();
        git.close();
        log.info("当前操作:完成执行:git add " + newFileName);
    }

    /**
     * git add command
     * @param git Git对象
     * @throws IOException
     * @throws GitAPIException
     */
    public void gitAddCommand(Git git) throws IOException, GitAPIException {
        log.info("当前操作:开始执行:git add .");
        git.add().addFilepattern(".").call();
        Status status = git.status().call();
        log.info("add文件列表:" + status.getUncommittedChanges());
        Set<String> added = status.getAdded();
        System.out.println(added);
        git.close();
        log.info("当前操作:完成执行:git add .");
    }

    /**
     * git commit command
     * @param git Git对象
     * @param gitCommitMessage git提交信息
     * @throws IOException
     * @throws GitAPIException
     */
    public void gitCommitCommand(Git git,String gitCommitMessage) throws IOException, GitAPIException {
        log.info("当前操作:开始执行:git commit -m '" + gitCommitMessage +"'");
        Status status = git.status().call();
        log.info("提交文件列表:" + status.getUncommittedChanges());
        //提交代码
        CommitCommand commitCommand = git.commit();
        commitCommand.setMessage(gitCommitMessage);
        commitCommand.call();
        git.close();
        log.info("当前操作:完成执行:git commit -m " + gitCommitMessage);
    }


    /**
     * git pull
     * @param git Git对象
     * @param usernamePasswordCredentialsProvider 用户信息
     * @throws IOException
     * @throws GitAPIException
     */
    public void gitPullCommand(Git git,UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider) throws IOException, GitAPIException {
        log.info("当前操作:开始执行:git pull");
        PullCommand pullCommand = git.pull();
        pullCommand.setRemoteBranchName("master");
        pullCommand.setCredentialsProvider(usernamePasswordCredentialsProvider);
        pullCommand.call();
        git.close();
        log.info("当前操作:完成执行:git pull");
    }

    /**
     * git push command
     * @param git Git对象
     * @param usernamePasswordCredentialsProvider 用户信息
     * @param operatorBranchName 操作分支名称
     * @throws IOException
     * @throws GitAPIException
     */
    public void gitPushCommand(Git git,UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider,String operatorBranchName) throws IOException, GitAPIException {
        log.info("当前操作:开始执行:git push");
        log.info("操作分支:" + operatorBranchName);
        PushCommand pushCommand = git.push();
        pushCommand.setRemote("origin");
        pushCommand.setCredentialsProvider(usernamePasswordCredentialsProvider).setPushTags();
        pushCommand.setRefSpecs(new RefSpec(operatorBranchName));
        pushCommand.call();
        git.close();
        log.info("当前操作:完成执行:git push");
    }

    /**
     * 从github/gitee上下载文件
     * @param git Git对象
     * @param baseGitRepositoryLocation 本地git仓库地址
     * @param usernamePasswordCredentialsProvider 用户信息
     * @param operatorBranchName 操作分支名称
     * @param remoteGitRepositoryUri 远程git仓库地址
     * @throws IOException
     * @throws GitAPIException
     * @throws URISyntaxException
     */
    public void downloadFromRemoteRepository(Git git,String baseGitRepositoryLocation,UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider,
            String operatorBranchName,String remoteGitRepositoryUri) throws IOException, GitAPIException, URISyntaxException {
        //删除之前clone到本地的仓库
        this.deleteRepository(baseGitRepositoryLocation);
        //从github克隆仓库
        this.gitCloneCommand(usernamePasswordCredentialsProvider,baseGitRepositoryLocation,remoteGitRepositoryUri,operatorBranchName);
    }

    /**
     * 将本地代码上传到github/gitee上
     * @param git Git对象
     * @param baseGitRepositoryLocation 本地仓库地址
     * @param gitCommitMessage git提交信息
     * @param usernamePasswordCredentialsProvider 用户信息
     * @param operatorBranchName 操作分支名称
     * @throws IOException
     * @throws GitAPIException
     * @throws URISyntaxException
     */
    public void uploadToRemoteRepository(Git git, String baseGitRepositoryLocation, String gitCommitMessage,
         UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider, String operatorBranchName) throws IOException, GitAPIException, URISyntaxException {

        //add文件:不创建新文件
        this.gitAddCommand(git);

        //add文件:创建新文件
//        String newFileName = "test.txt";
//        this.gitAddCommand(git,newFileName,baseGitRepositoryLocation,operatorBranchName);

        //提交文件
        this.gitCommitCommand(git,gitCommitMessage);
        //push到远程
        this.gitPushCommand(git,usernamePasswordCredentialsProvider,operatorBranchName);
    }

}



//public class GitUtil {

//    //切换分支
//    public void checkoutBranch(String localPath, String branchName){
//        String projectURL = localPath + "\\.git";
//
//        Git git = null;
//        try {
//            git = Git.open(new File(projectURL));
//            git.checkout().setCreateBranch(true).setName(branchName).call();
//            git.pull().call();
//            System.out.println("切换分支成功");
//        }catch (Exception e){
//            e.printStackTrace();
//            System.out.println("切换分支失败");
//        } finally{
//            if (git != null) {
//                git.close();
//            }
//        }
//    }
//

//    public static void main(String[] args) {
//        GitUtil gitUtil = new GitUtil();
//        //git远程url地址
//        String url = "XXXX.git";
////        String localPath = "d:/jgitTest";
//        String localPath = System.getProperty("user.dir");
//        String branchName = "20171010_branch";
//        try {
////            gitUtil.cloneRepository(url,localPath);
////            gitUtil.checkoutBranch(localPath,branchName);
//            gitUtil.commit(localPath,"测试提交1");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}

