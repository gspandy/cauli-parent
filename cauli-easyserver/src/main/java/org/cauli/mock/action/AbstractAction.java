package org.cauli.mock.action;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jodd.util.StringUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cauli.common.instrument.ClassUtils;
import org.cauli.common.keyvalue.KeyValueStore;
import org.cauli.common.keyvalue.KeyValueStores;
import org.cauli.common.keyvalue.NormalSortComparator;
import org.cauli.mock.*;
import org.cauli.common.keyvalue.ValueHandler;
import org.cauli.mock.annotation.Action;
import org.cauli.mock.annotation.CallBack;
import org.cauli.mock.context.Context;
import org.cauli.mock.data.DataProviderBuilder;
import org.cauli.mock.data.IDataProvider;
import org.cauli.mock.entity.*;
import org.cauli.mock.exception.ActionExecuteException;
import org.cauli.mock.exception.ServerNameNotSupportChineseException;
import org.cauli.mock.server.MockServer;
import org.cauli.mock.strategy.IStrategy;
import org.cauli.mock.template.TemplateSourceEngine;
import org.cauli.mock.util.CommonUtil;
import org.cauli.mock.util.TemplateParseUtil;
import org.cauli.server.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @auther sky
 */
public abstract class AbstractAction<T,V> implements MockAction<String,ParametersModel,V>,IStrategy<T>{

    private TemplateSourceEngine sourceEngine;

    private FixedSizeBox<ParametersModel> fixedSizeBox = new FixedSizeBox<ParametersModel>();

    private T request;

    public T request() {
        return request;
    }

    private IDataProvider provider= DataProviderBuilder.getInstance().getDataProvider();

    public void setRequest(T request) {
        this.request = request;
    }

    private Set<Method> parameterConfigMethods= Sets.newHashSet();

    private Set<Method> templateConfigMethods=Sets.newHashSet();

    private ParametersModel parametersModel;

    private Map<String,Method> callbacks= Maps.newHashMap();

    private MockServer server;

    private Logger logger = LoggerFactory.getLogger(AbstractAction.class);

    private ActionInfo actionInfo=new ActionInfo();

    public abstract void config(ActionInfo actionInfo);

    public AbstractAction(String name){
        actionInfo.setActionName(name);
        parseMethods();
        config(actionInfo);
//        if(CommonUtil.checkContainsChinese(actionInfo.getActionName())){
//            throw new ServerNameNotSupportChineseException("ActionName不支持非英文:"+actionInfo.getActionName());
//        }
        if(actionInfo.getTemplateSourceLoaderClass()!=null){
            this.sourceEngine=checkTemplateSourceEngineClass(actionInfo.getTemplateSourceLoaderClass());
        }
        if(StringUtils.isEmpty(getRequestUri())){
            this.setRequestUri("/"+getActionName());
        }
    }


    public void addContext(String name,Object object){
        this.parametersModel.getContext().addObject(name,object);
    }

    public KeyValueStores loadData(String section,Comparator comparator){
        return provider.loadDatas(parametersModel.getContext(),getDefaultDataProviderFile(),section,comparator,new ValueHandler() {
            @Override
            public Object transfer(Object value) {
                return value;
            }
        });
    }

    public KeyValueStores loadData(String section){
        return provider.loadDatas(parametersModel.getContext(),getDefaultDataProviderFile(),section,new NormalSortComparator(),new ValueHandler() {
            @Override
            public Object transfer(Object value) {
                return value;
            }
        });
    }

    public KeyValueStores loadData(String fileName,String section){
        return provider.loadDatas(parametersModel.getContext(),fileName,section,new NormalSortComparator(),new ValueHandler() {
            @Override
            public Object transfer(Object value) {
                return value;
            }
        });
    }

    public KeyValueStores loadData(File file,String section){
        return provider.loadDatas(parametersModel.getContext(),file.getAbsolutePath(),section,new NormalSortComparator(),new ValueHandler() {
            @Override
            public Object transfer(Object value) {
                return value;
            }
        });
    }

    public KeyValueStores loadData(File file,String section,Comparator comparator){
        return provider.loadDatas(parametersModel.getContext(),file.getAbsolutePath(),section,comparator,new ValueHandler() {
            @Override
            public Object transfer(Object value) {
                return value;
            }
        });
    }

    public KeyValueStores loadData(String section,ValueHandler valueHandler){
        return provider.loadDatas(parametersModel.getContext(),getDefaultDataProviderFile(),section,new NormalSortComparator(),valueHandler);
    }

    public KeyValueStores loadData(String fileName,String section,ValueHandler valueHandler){
        return provider.loadDatas(parametersModel.getContext(),fileName,section,new NormalSortComparator(),valueHandler);
    }

    public KeyValueStores loadData(File file,String section,ValueHandler valueHandler){
        return provider.loadDatas(parametersModel.getContext(),file.getAbsolutePath(),section,new NormalSortComparator(),valueHandler);
    }

    public KeyValueStores loadData(File file,String section,Comparator comparator,ValueHandler valueHandler){
        return provider.loadDatas(parametersModel.getContext(),file.getAbsolutePath(),section,comparator,valueHandler);
    }


    public String getCallbackReturnStatus(){
        return actionInfo.getCallbackInfo().getReturnStatus();
    }

    public String getCallbackTemplateValue(){
        return this.sourceEngine.getCallbackTemplates().get(getCallbackReturnStatus());
    }


    public String getCallbackTemplateValue(String status){
        return this.sourceEngine.getCallbackTemplates().get(status);
    }

    @Override
    public void onMessage(ParametersModel pairs) {

    }

    @Override
    public String build() throws Exception {
        runParameterConfigs();
        actionInfo.setReturnStatus(process(request,parametersModel.getContext()));
        if(getActionInfo().isUseTemplate()){
            logger.info("[{}:{}]请求的模板状态为:{}",getServerName(),getActionName(),getReturnStatus());
            String content= TemplateParseUtil.getInstance().toString(getParametersModel().getContext().getValues(),getTemplateValue(getReturnStatus()));
            if(!getActionInfo().getTemplateEncoding().equalsIgnoreCase("utf-8")){
                content= IOUtils.toString(IOUtils.toInputStream(content, getActionInfo().getTemplateEncoding()));
            }
            logger.info("[{}:{}]获取的Template的值:{}",getServerName(),getActionName(), content);
            setTemplateValue(content);
        }
        runTemplateConfig();
        logger.info("[{}:{}]响应设置timeout时间:{}",getServerName(),getActionName(), getActionInfo().getTimeoutMS());
        Thread.sleep(getActionInfo().getTimeoutMS()==0?0:getActionInfo().getTimeoutMS());
        return getParametersModel().getTemplateValue();
    }

    @Override
    public MockServer getServer() {
        return server;
    }

    @Override
    public String getReturnStatus() {
        return actionInfo.getReturnStatus();
    }

    @Override
    public String getActionName() {
        return actionInfo.getActionName();
    }

    public void setServer(MockServer server) {
        this.server = server;
    }

    public String getRequestUri(){
        return actionInfo.getRequestUri();
    }

    public String getServerName(){
        return this.server.getServerName();
    }

    public String getTemplateEncoding(){
        return actionInfo.getTemplateEncoding();
    }


    public long getTimeoutMS(){
        return actionInfo.getTimeoutMS();
    }

    public void setRequestUri(String requestUri){
        this.actionInfo.setRequestUri(requestUri);
    }

    @Override
    public ActionInfo getActionInfo() {
        return actionInfo;
    }

    @Override
    public String getTemplateValue(String status) {
        return this.sourceEngine.getTemplate(status);
    }

    @Override
    public void addTemplate(String returnStatus, String content) {
        logger.info("[{}:{}]增加状态:{}",getServerName(),getActionName(),returnStatus);
        String templateName = getTemplateName(returnStatus);
        logger.info("[{}:{}]增加模板:{}",getServerName(),getActionName(),templateName);
        this.sourceEngine.createTemplate(returnStatus,content);
    }

    @Override
    public Map<String, String> getTemplateStatuses() {
        Map<String,String> map =  this.sourceEngine.getActionTemplates();
        if(map==null||map.size()==0){
            map.put("SUCCESS","");
        }
        return map;
    }

    @Override
    public void load() {
        this.sourceEngine.init();
    }

    private String getTemplateName(String status){
        return getServer().getServerName()+"_"+getActionName()+"_"+status;
    }


    public void runParameterConfigs() throws ActionExecuteException {
        for(Method method:this.parameterConfigMethods){
            invokeConfigMethod(method);
        }

    }

    public void runTemplateConfig() throws ActionExecuteException {
        for(Method method:this.templateConfigMethods){
            invokeConfigMethod(method);
        }
    }

    private void invokeConfigMethod(Method method) throws ActionExecuteException {
        if(method!=null){
            ActionExecuter actionExcuter = new ActionExecuter(method,parametersModel,this);
            actionExcuter.invoke();
        }

    }

    public ParametersModel getParametersModel() {
        return parametersModel;
    }

    public void setParametersModel(ParametersModel parameterValuePairs) {
        this.parametersModel = parameterValuePairs;
    }

    public void parseMethods(){
        Method[] methods = getClass().getDeclaredMethods();
        for(Method method:methods){
            if(method.isAnnotationPresent(Action.class)){
                Action action = method.getAnnotation(Action.class);
                if(action.value()== ConfigType.PARAMETERS){
                    this.parameterConfigMethods.add(method);
                }else if(action.value()==ConfigType.TEMPLATE){
                    this.templateConfigMethods.add(method);
                }
            }else if(method.isAnnotationPresent(CallBack.class)){
                if(method.getReturnType()!=List.class){
                    throw new RuntimeException(getServerName()+":"+getActionName()+":CallBack注解的方法返回值只能够是List类型(Http和Socket返回List<String>类型)");
                }
                CallBack back = method.getAnnotation(CallBack.class);
                String name =back.value();
                callbacks.put(name,method);
            }
        }
    }

    public List<V> callback(String name) throws ActionExecuteException {
        List<V> result = Lists.newArrayList();
        Method method = callbacks.get(name);
        if(method==null){
            throw new RuntimeException("["+getServerName()+":"+getActionName()+"]"+"找不到该名字["+name+"]的Callback方法");
        }
        ActionExecuter executer = new ActionExecuter(callbacks.get(name),parametersModel,this);
        List<V> callbackResult = (List<V>) executer.invoke();
        logger.info("[{}:{}]得到的callback的内容:{}",getServerName(),getActionName(),callbackResult);
        result.addAll(callbackResult);
        return result;
    }


    public void delay(int seconds){
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
            logger.warn("[{}:{}]delay 被打断...",getServerName(),getActionName());
        }
    }



    public String getTemplateValue(){
        if(StringUtil.isEmpty(this.parametersModel.getTemplateValue())){
            return this.sourceEngine.getTemplate(actionInfo.getReturnStatus());
        }
        return this.parametersModel.getTemplateValue();
    }

    public void setTemplateValue(String content){
        this.parametersModel.setTemplateValue(content);
    }

    public String callbackContent() {
        try{
            return TemplateParseUtil.getInstance().toString(parametersModel.getContext().getValues(),getCallbackTemplateValue());
        }catch (Exception e){
            logger.error("[{}:"+getActionName()+"]"+"获取callback的模板内容出错",getServerName(),e);
            return null;
        }
    }

    private TemplateSourceEngine checkTemplateSourceEngineClass(Class<? extends TemplateSourceEngine> clazz){
        try {
            Constructor constructor=clazz.getConstructor(MockAction.class);
            return (TemplateSourceEngine) constructor.newInstance(this);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(getServerName()+":"+getActionName()+"加载模板运行器的时候出现了错误,构造方法只能够有一个参数MockAction.class,class:"+clazz.getName(),e);
        } catch (Exception e) {
            throw new RuntimeException(getServerName()+":"+getActionName()+"构造模板运行期的时候出现了系统异常",e);
        }
    }

    protected String getDefaultDataProviderFile(){
        return "data"+ "/"+getServerName()+"/"+actionInfo.getActionName()+".props";
    }



    @Override
    public String process(T request, Context context) {
        return actionInfo.getReturnStatus();
    }


    public Context getContext(){
        return this.parametersModel.getContext();
    }

    public String getContextText(String key){
        return getContext().getString(key);
    }


    public TemplateSourceEngine getSourceEngine() {
        return sourceEngine;
    }

    public void setSourceEngine(TemplateSourceEngine sourceEngine) {
        this.sourceEngine = sourceEngine;
    }

    public IDataProvider getProvider() {
        return provider;
    }

    public void setProvider(IDataProvider provider) {
        this.provider = provider;
    }

    @Override
    public void addContext(KeyValueStore store) {
        this.parametersModel.getContext().addContext(store.getKey(),store.getValue());
    }

    public Set<String> getAllCallbacks(){
        return callbacks.keySet();
    }

    @Override
    public void updateTemplateValue(String returnStatus,String value) {
        this.sourceEngine.updateTemplate(returnStatus,value);
    }

    @Override
    public void updateCallbackTemplateValue(String returnStatus, String value) {
        this.sourceEngine.updateCallbackTemplate(returnStatus,value);
    }

    @Override
    public Set<String> getCallbackReturnStatuses() {
        Set<String> callbackReturnStatuses = this.sourceEngine.getCallbackTemplates().keySet();
        if(callbackReturnStatuses==null||callbackReturnStatuses.size()==0){
            return Sets.newHashSet("SUCCESS");
        }else{
            return callbackReturnStatuses;
        }
    }

    public synchronized void addRequestHistory(ParametersModel parametersModel){
        String now =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        fixedSizeBox.add(new KeyValueStore(now,parametersModel));
    }

    public List<V> callback(String name,String date) throws ActionExecuteException {
        ParametersModel model = fixedSizeBox.get(date);
        List<V> result = Lists.newArrayList();
        Method method = callbacks.get(name);
        if(method==null){
            throw new RuntimeException("["+getServerName()+":"+getActionName()+"]"+"找不到该名字["+name+"]的Callback方法");
        }
        ActionExecuter executer = new ActionExecuter(callbacks.get(name),model,this);
        List<V> callbackResult = (List<V>) executer.invoke();
        logger.info("[{}:{}]得到的callback的内容:{}",getServerName(),getActionName(),callbackResult);
        result.addAll(callbackResult);
        return result;
    }


    @Override
    public List<String> getRequestHistoryDates() {
        return this.fixedSizeBox.getKeys();
    }



}
