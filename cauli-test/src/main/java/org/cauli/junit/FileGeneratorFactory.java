package org.cauli.junit;

import org.cauli.instrument.ClassPool;
import org.cauli.junit.ExcelGenerator;
import org.cauli.junit.FileGenerator;
import org.cauli.junit.TXTGenerator;
import org.cauli.junit.anno.FileParse;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by tianqing.wang on 14-3-18
 */
public class FileGeneratorFactory {

    public static FileGenerator createFileGenerator(File file,Method method,String readType){
        if(file.getName().endsWith("txt")){
            FileGenerator fileGenerator=new TXTGenerator(file,readType);
            return fileGenerator;
        }else if(file.getName().endsWith("xls")||file.getName().endsWith("xlsx")){
            FileGenerator fileGenerator=new ExcelGenerator(file,readType,method);
            return fileGenerator;
        }else{
            Set<Class<?>> classes = ClassPool.getClassPool();
            for(Class<?> clazz :classes){
                if(clazz.isAnnotationPresent(FileParse.class)){
                    FileParse fileParse =clazz.getAnnotation(FileParse.class);
                    String suffix = fileParse.suffix();
                    if(file.getName().endsWith(suffix)){
                        try {
                            Constructor constructor =clazz.getConstructor(File.class,String.class);
                            return (FileGenerator) constructor.newInstance(file,readType);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException("查找参数化解析类的时候出现了错误...");
                        }
                    }
                }
            }
        }
        return null;
    }
    public static FileGenerator createFileGenerator(File file,Method method){
        return createFileGenerator(file,method,"row");
    }
}
