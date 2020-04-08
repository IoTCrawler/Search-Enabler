//package com.agtinternational.iotcrawler.graphqlEnabler;
//
//import java.lang.reflect.InvocationHandler;
//import java.lang.reflect.Method;
//import java.lang.reflect.Proxy;
//
//public class ProxyWrapper<T> implements InvocationHandler {
//
//    Class<T> clazz = null;
//    T myvalue = null;
//
//    public interface Foo {
//        Object bar(Object obj) throws Exception;
//    }
//
//    public class FooImpl implements Foo {
//        public Object bar(Object obj) throws Exception{
//            String test = "123";
//            return null;
//        }
//    }
//
//    public static <W,T> W getInstance(Class<W> clazz, Class<T> clazz2) {
//        ProxyWrapper<T> wrapper = new ProxyWrapper<T>();
//        wrapper.setClass(clazz2);
//        @SuppressWarnings("unchecked")
//        W proxy = (W) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, wrapper);
//        return proxy;
//    }
//
//    private void setClass(Class<T> clazz) {
//        this.clazz = clazz;
//    }
//
//    public Object invoke(Object proxy, Method method, Object[] args)
//            throws Throwable {
//        // getter has no arguments
//        if (method.getName().startsWith("get") && (args == null || args.length == 0)) {
//            return myvalue;
//        } else if (method.getName().startsWith("set") && args.length == 1) {
//            Object o = args[0];
//            if (o.getClass().isAssignableFrom(clazz)) {
//                @SuppressWarnings("unchecked")
//                T val = (T)o;
//                myvalue = val;
//                return null;
//            }
//        } else {
//            throw new Exception();
//        }
//        return null;
//    }
//
//
//}