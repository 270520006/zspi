package cn.zsp.spi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
/**
 * @author zsp
 * @version 1.0
 * @date 2021/11/26 15:44
 * 只是仿造Java SPI源码造的轮子，没有考虑多线程
 * 安全策略等问题，后续会继续完善
 */
public final class ZspLoader<S>  {
    //写死读取配置文件的路径
    private static final String PREFIX = "META-INF/services/";
    //获取类对象
    private  Class<S> service;
    //获取类加载器
    private  ClassLoader loader;
    // 缓存，键是配置文件的实现类名字，值是反射获取的对象
    private LinkedHashMap<String,S> cacheObjectMap = new LinkedHashMap<>();
    // 缓存，存储已经生成过的对象，键是接口名字，值是反射对象的集合
    private HashMap<String,LinkedHashMap<String, S>> cacheInterfaceMap=new HashMap<String,LinkedHashMap<String, S>>() ;
    //用于存储配置文件位置
    Enumeration<URL> configs;

    public ZspLoader(Class<S> service) {
        //直接获取到传参
        this.service = service;
        this.loader = service.getClassLoader();
    }
    public <S> LinkedHashMap<String,S> load(){

         if (cacheInterfaceMap!=null&&!cacheInterfaceMap.isEmpty()){
            LinkedHashMap<String, S> linkedHashMap = (LinkedHashMap<String, S>) cacheInterfaceMap.get(service.getName());
            return linkedHashMap;
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return (LinkedHashMap<String, S>) load(service, cl);
    }

    private  <S> LinkedHashMap<String,S> load(Class<S> service, ClassLoader cl) {
        //判断class是否为空
        service = Objects.requireNonNull(service, "Service interface cannot be null");
        //判断类加载器是否为空，为空则使用系统类加载器
        loader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        //从这里开始执行Java SPI的核心内容

        try {
            //1、使用流获取配置文件中的实现类路径
            ArrayList names=parse();
            //2、使用反射对上述文件记录对象进行生成
            getObject(names);
            //3、返回实现类名和对象的map
            cacheInterfaceMap.put(service.getName(),cacheObjectMap);
           return (LinkedHashMap<String, S>) cacheObjectMap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (LinkedHashMap<String, S>) cacheObjectMap;
    }

    private void getObject(ArrayList<String> names) {
        //遍历配置文件中的名字集合
        for (String name : names) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                S p = service.cast(c.newInstance());
                cacheObjectMap.put(name,p);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

    }

    private ArrayList<String> parse() throws IOException {
        //拼串获取配置文件
        String fullName=PREFIX + service.getName();

        //创建方法集，用来存方法
        ArrayList<String> names = new ArrayList<>();
            if (loader == null) {
                configs = ClassLoader.getSystemResources(fullName);
            } else {
                configs = loader.getResources(fullName);
            }
        URL url = null;
        try {
            url = configs.nextElement();
        } catch (Exception e) {
            throw  new IOException("你的META-INF/services/目录下没有文件！");
        }

        return  parseObjectList(url);
    }

    private ArrayList parseObjectList(URL url) {
        //使用流将配置文件中的所有类位置读取出来
        //获取每一个类是使用parseLine方法
        InputStream in = null;
        BufferedReader r = null;
        ArrayList<String> names = new ArrayList<>();
        try {
            in = url.openStream();
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            int lc = 1;
            while ((lc = parseLine(service, url, r, lc, names)) >= 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return names;
    }

    private int parseLine(Class<S> service, URL url, BufferedReader r, int lc, ArrayList<String> names) throws IOException {
        //获取类名，需要判断是否含有除了.以外的符号
        String ln=r.readLine();
        if (ln==null)
        {
            return -1;
        }
        int ci=ln.indexOf('#');
        if (ci>=0) ln=ln.substring(0,ci);
        ln=ln.trim();
        int n=ln.length();
        if (ln.indexOf(' ')>0||ln.indexOf('\t')>=0){
            throw new IOException("写个实体类的路径，带空格带换行的算怎么回事");
        }
        int cp =ln.codePointAt(0);
        if(!Character.isJavaIdentifierStart(cp)){
            throw new IOException("写个实体类的路径，开头居然是符号，谁教你这么写的");
        }
        for (int i = Character.charCount(cp); i <n ; i+=Character.charCount(cp)) {
            cp = ln.codePointAt(i);
            if (!Character.isJavaIdentifierPart(cp) && (cp != '.')){
                throw new IOException("写个实体类的路径，中间还有其他符号，回去重学打字吧");
            }
        }
        names.add(ln);
        return lc+1;
    }


}
