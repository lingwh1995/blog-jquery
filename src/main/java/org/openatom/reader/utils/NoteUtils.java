package org.openatom.reader.utils;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.regex.Pattern;

public class NoteUtils {

    @Test
    public void test() throws IOException {
        String noteFilesLocation = System.getProperty("user.dir") + "\\notes";
        Yaml yaml = new Yaml();
        JSONObject application = yaml.loadAs(NoteUtils.class.getResourceAsStream("/application.yml"), JSONObject.class);
        JSONObject git = application.getJSONObject("git");
        String htmlFilesLocation = git.getJSONObject("htmlFilesLocation").getJSONObject("github").get("pages").toString();
        NoteUtils noteUtils = new NoteUtils();
        noteUtils.convertAllTxtNotesToHtml(noteFilesLocation,htmlFilesLocation);
    }

    /**
     * 为所有txt格式文件生成html文件
     * @return
     */
    public String convertAllTxtNotesToHtml(String noteFilesLocation,String htmlFilesLocation) throws IOException {
        //创建存放生成html文件的文件夹
        File noteFilesDirectory = new File(noteFilesLocation);
        File htmlFilesDirectory = new File(htmlFilesLocation);
        if(htmlFilesDirectory.exists()){
            FileUtils.forceDelete(htmlFilesDirectory);
        }
        FileUtils.forceMkdir(htmlFilesDirectory);
        for (File file : noteFilesDirectory.listFiles()) {
            String srcTxtFileLocation = file.getPath();
            String targetHtmlFileLocation = htmlFilesLocation+ "\\" + file.getName().replace(".txt",".html");
            convertSingleTxtNoteToHtml(srcTxtFileLocation,targetHtmlFileLocation);
        }
        return "";
    }

    /**
     * 读取一个txt文件生成对应的html文件
     * @param srcTxtFileLocation
     * @param targetHtmlFileLocation
     * @return
     */
    private String convertSingleTxtNoteToHtml(String srcTxtFileLocation,String targetHtmlFileLocation) {
        BufferedReader reader = null;
        //生成内容
        BufferedWriter writer = null;
        JSONArray firstLevelnavMenu = new JSONArray();
        try {
            reader = new BufferedReader(new FileReader(srcTxtFileLocation));
            writer = new BufferedWriter(new FileWriter(targetHtmlFileLocation));
            String currentLine= "";
            String firstLevelElementId = "";
            String secondLevelElementId = "";
            String thirdLevelElementId = "";
            String fourthLevelElementId = "";
            String fifthLevelElementId = "";

            JSONArray secondLevelnavMenu = new JSONArray();
            JSONArray thirdLevelnavMenu = new JSONArray();
            JSONArray fourthLevelnavMenu = new JSONArray();
            while ((currentLine = reader.readLine()) != null) {
                //各级标题匹配表达式
                String firstLelevTiltleRegExpression = "^[1-9][0-9]?\\.";
                String secondLelevTiltleRegExpression = "^\\s+[1-9][0-9]?>\\.";
                String thirdLelevTiltleRegExpression = "^\\s+[1-9][0-9]?>>\\.";
                String fourthLelevTiltleRegExpression = "^\\s+[1-9][0-9]?>>>\\.";
                String fifthLelevTiltleRegExpression = "^\\s+[1-9][0-9]?>>>>\\.";
                //一级标题匹配器
                boolean isMatchFirstLevelTitle = Pattern.compile(firstLelevTiltleRegExpression).matcher(currentLine).find();
                boolean isMatchSecondLevelTitle = Pattern.compile(secondLelevTiltleRegExpression).matcher(currentLine).find();
                boolean isMatchThirdLevelTitle = Pattern.compile(thirdLelevTiltleRegExpression).matcher(currentLine).find();
                boolean isMatchFourthLevelTitle = Pattern.compile(fourthLelevTiltleRegExpression).matcher(currentLine).find();
                boolean isMatchFifthLevelTitle = Pattern.compile(fifthLelevTiltleRegExpression).matcher(currentLine).find();
                if(isMatchFirstLevelTitle || isMatchSecondLevelTitle || isMatchThirdLevelTitle || isMatchFourthLevelTitle ||isMatchFifthLevelTitle) {

                    //提取一级标题
                    if(isMatchFirstLevelTitle) {
                        secondLevelnavMenu = new JSONArray();
                        //格式化,去除空格
                        String currentLineTrim =  currentLine.trim();
                        //拼接元素id
                        firstLevelElementId = currentLineTrim.substring(0, currentLineTrim.indexOf(".")+1);
                        //拼接内容相关html
                        String currentLineForContent = "<span class=\".white-space\" id='" + firstLevelElementId + "'>" + currentLine + "</span>";
                        //给文件中写入内容相关html
                        contentWriter(writer, currentLineForContent);
                        JSONObject firstLevelNavMenuItem = new JSONObject();
                        firstLevelNavMenuItem.put("name",currentLineTrim);
                        firstLevelNavMenuItem.put("open",false);
                        firstLevelNavMenuItem.put("navMenuItemId",firstLevelElementId);
                        firstLevelnavMenu.add(firstLevelNavMenuItem);
                    }

                    //提取二级标题
                    if(isMatchSecondLevelTitle) {
                        thirdLevelnavMenu = new JSONArray();
                        //格式化,去除空格
                        String currentLineTrim =  currentLine.trim();
                        //拼接元素id
                        secondLevelElementId = firstLevelElementId + currentLineTrim.substring(0, currentLineTrim.indexOf(".")+1);
                        //拼接内容相关html
                        String currentLineForContent = "<span class=\".white-space\" id='" + secondLevelElementId + "'>" + currentLine + "</span>";
                        //给文件中写入内容相关html
                        contentWriter(writer, currentLineForContent);

                        //获取当前二级菜单的一级菜单
                        JSONObject currentFirstLevelMenu = (JSONObject)firstLevelnavMenu.get(firstLevelnavMenu.size()-1);
                        JSONObject secondLevelNavMenuItem = new JSONObject();
                        secondLevelNavMenuItem.put("name",currentLineTrim);
                        secondLevelNavMenuItem.put("open",false);
                        secondLevelNavMenuItem.put("navMenuItemId",secondLevelElementId);
                        secondLevelnavMenu.add(secondLevelNavMenuItem);
                        currentFirstLevelMenu.put("children",secondLevelnavMenu);
                    }
                    //提取三级标题
                    if(isMatchThirdLevelTitle) {
                        fourthLevelnavMenu = new JSONArray();
                        //格式化,去除空格
                        String currentLineTrim =  currentLine.trim();
                        //拼接元素id
                        thirdLevelElementId = secondLevelElementId + currentLineTrim.substring(0, currentLineTrim.indexOf(".")+1);
                        //拼接内容相关html
                        String currentLineForContent = "<span class=\".white-space\" id='" + thirdLevelElementId + "'>" + currentLine + "</span>";
                        //给文件中写入内容相关html
                        contentWriter(writer, currentLineForContent);

                        //获取当前三级菜单的二级菜单
                        JSONObject currentSecondLevelMenu = (JSONObject)secondLevelnavMenu.get(secondLevelnavMenu.size()-1);
                        JSONObject thirdLevelNavMenuItem = new JSONObject();
                        thirdLevelNavMenuItem.put("name",currentLineTrim);
                        thirdLevelNavMenuItem.put("open",false);
                        thirdLevelNavMenuItem.put("navMenuItemId",secondLevelElementId);
                        thirdLevelnavMenu.add(thirdLevelNavMenuItem);
                        currentSecondLevelMenu.put("children",thirdLevelnavMenu);
                    }

                    //提取四级标题
                    if(isMatchFourthLevelTitle) {
                        //格式化,去除空格
                        String currentLineTrim =  currentLine.trim();
                        //拼接元素id
                        fourthLevelElementId = thirdLevelElementId + currentLineTrim.substring(0, currentLineTrim.indexOf(".")+1);
                        //拼接内容相关html
                        String currentLineForContent = "<span class=\".white-space\" id='" + fourthLevelElementId + "'>" + currentLine + "</span>";
                        //给文件中写入内容相关html
                        contentWriter(writer, currentLineForContent);
                        //获取当前四级菜单的三级菜单
                        JSONObject currentThirdLevelMenu = (JSONObject)thirdLevelnavMenu.get(thirdLevelnavMenu.size()-1);
                        JSONObject fourthLevelNavMenuItem = new JSONObject();
                        fourthLevelNavMenuItem.put("name",currentLineTrim);
                        fourthLevelNavMenuItem.put("open",false);
                        fourthLevelNavMenuItem.put("navMenuItemId",secondLevelElementId);
                        fourthLevelnavMenu.add(fourthLevelNavMenuItem);
                        currentThirdLevelMenu.put("children",fourthLevelnavMenu);
                    }

                    //提取五级标题
                    if(isMatchFifthLevelTitle) {
                        //格式化,去除空格
                        String currentLineTrim =  currentLine.trim();
                        //拼接元素id
                        fifthLevelElementId = fourthLevelElementId + currentLineTrim.substring(0, currentLineTrim.indexOf(".")+1);
                        //拼接内容相关html
                        String currentLineForContent = "<span class=\".white-space\" id='" + fifthLevelElementId + "'>" + currentLine + "</span>";
                        contentWriter(writer, currentLineForContent);
                    }
                }else{
                    //给文件中写入内容相关html
                    contentWriter(writer, currentLine);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(reader!=null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(writer!=null){
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return firstLevelnavMenu.toString();
    }

    /**
     * 将读取的内容写入到内容文件中
     * @param contentWriter
     * @param currentLine
     * @throws IOException
     */
    private static void contentWriter(BufferedWriter contentWriter, String currentLine) throws IOException {
        contentWriter.write(currentLine);
        contentWriter.newLine();
        //清除缓存
        contentWriter.flush();
    }
}
