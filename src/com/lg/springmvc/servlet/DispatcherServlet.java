package com.lg.springmvc.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.lg.springmvc.annotation.Autowired;
import com.lg.springmvc.annotation.Controller;
import com.lg.springmvc.annotation.RequestMapping;
import com.lg.springmvc.annotation.RequestParam;
import com.lg.springmvc.annotation.Service;

/**
 * MVC前端控制器
 * @author xulg 2017年8月21日
 */
public class DispatcherServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	// 存放需要被实例化的类名的集合
	private List<String> classNames = new ArrayList<>();

	// 存放实例化对象映射关系的容器
	private Map<String, Object> beans = new HashMap<>();

	// 存放url和请求方法的映射关系的集合
	private Map<String, HandlerModel> handlerMapping = new HashMap<>();

	@Override
	public void init() throws ServletException {
		System.out.println(this.getClass().getName() + " 前端控制器开始初始化!");
		// 扫描包, 获取需要实例化的组件的类名
		scanPackage(getInitParameter("scanPackage"));
		// 实例化组件
		doInstance();
		// 属性注入
		doAutoWired();
		try {
			// 建立url映射
			doHandlerMapping();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void scanPackage(String scanPackage) throws ServletException {
		if (scanPackage == null || scanPackage.trim().equals("")) {
			throw new ServletException("找不到包路径");
		}
		// 包路径转为目录结构
		String pathname = "/" + scanPackage.replaceAll("\\.", "/");
		URL url = this.getClass().getClassLoader().getResource(pathname);
		File files = new File(url.getFile());
		// 递归查询所有的class文件
		for (File file : files.listFiles()) {
			// 是目录
			if (file.isDirectory()) {
				scanPackage(scanPackage + "." + file.getName());
			} else {
				String fileName = file.getName();
				// 判断是否是.class文件
				if (!fileName.endsWith(".class")) {
					continue;
				} else {
					// 判断是class文件, 获取该文件的包名
					String className = scanPackage.replace("/", "\\.") + "." + fileName.replace(".class", "");
					try {
						Class<?> clazz = Class.forName(className);
						// 判断是否有@Controller, @Service注解, 是则保存类名
						if (clazz.isAnnotationPresent(Controller.class)
							|| clazz.isAnnotationPresent(Service.class)) {
							classNames.add(className);
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void doInstance() throws ServletException {
		if (classNames.size() == 0) {
			return;
		}
		try {
			for (String className : classNames) {
				Class<?> clazz = Class.forName(className);
				// 实例化该类
				Object bean = clazz.newInstance();
				// 是@Controller注解
				if (clazz.isAnnotationPresent(Controller.class)) {
					String beanName = lowerFirstChar(clazz.getSimpleName());
					// 类名为key
					beans.put(beanName, bean);
				}
				// 是@Service注解
				if (clazz.isAnnotationPresent(Service.class)) {
					// 获取@Service注解对象
					Service service = clazz.getAnnotation(Service.class);
					// 获取@Service注解的value, 用于作为key
					String beanName = service.value();
					// 若beanName为空, 则用接口的名称首字母小写作为key;若没有接口, 则使用类名首字母小写作为key
					if (beanName == null || "".equals(beanName.trim())) {
						Class<?>[] interfaces = clazz.getInterfaces();
						for (Class<?> face : interfaces) {
							// 接口名称和实现类名称相近
							if (clazz.getSimpleName().startsWith(face.getSimpleName())) {
								beanName = lowerFirstChar(face.getSimpleName());
								break;
							}
						}
						if (beanName == null || "".equals(beanName.trim())) {
							beanName = lowerFirstChar(clazz.getSimpleName());
						}
					}
					beans.put(beanName, bean);
				}
				System.out.println("实例化了Bean： " + clazz.getName());
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new ServletException(e.getMessage());
		}
	}

	// 首字母小写
	private String lowerFirstChar(String className) {  
        char[] chars = className.toCharArray();  
        chars[0] += 32;  
        return String.valueOf(chars);  
    }

	private void doAutoWired() {
		if (beans.size() == 0) {
			return;
		}
		try {
			for (Entry<String, Object> entry : beans.entrySet()) {
				Object bean = entry.getValue();
				// 获取bean的所有的字段, 包括public, private, proteced, 但是不包括父类的申明字段
				Field[] fields = bean.getClass().getDeclaredFields();
				if (fields != null && fields.length > 0) {
					for (Field field : fields) {
						// 判断当前字段使用了@Autowired注解, 没使用则结束本次循环
						if (!field.isAnnotationPresent(Autowired.class)) {
							continue;
						}
						// 获取@Autowired注解对象
						Autowired autowired = field.getAnnotation(Autowired.class);
						// 获取autowired对象的value作为beanName
						String beanName = autowired.value();
						// autowired对象的value为空, 则使用field的类型的类名的首字母小写作为beanName
						if (beanName == null || "".equals(beanName)) {
							beanName = lowerFirstChar(field.getType().getSimpleName());
						}
						// 从容器中根据beanName查找bean对象
						Object fieldValue = beans.get(beanName);
						if (fieldValue != null) {
							// 将私有化的属性设为true, 不然访问不到
			                field.setAccessible(true);
			                // 给当前的field字段设置值
							field.set(bean, fieldValue);
						}
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private void doHandlerMapping() throws IOException {
		if (beans.size() == 0) {
			return;
		}
		// 寻找所有的Controller类
		for (Entry<String, Object> entry : beans.entrySet()) {
			Object bean = entry.getValue();
			Class<? extends Object> clazz = bean.getClass();
			// 不是controller对象
			if (!clazz.isAnnotationPresent(Controller.class)) {
				continue;
			}
			StringBuilder url = new StringBuilder("/");
			// 获取controller对象上的@RequestMapping
			if (clazz.isAnnotationPresent(RequestMapping.class)) {
				RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
				// 获取映射路径的值, 拼接到url上
				url.append(requestMapping.value());
			}
			// 获取controller对象的所有的public的method方法
			Method[] methods = clazz.getMethods();
			if (methods != null && methods.length > 0) {
				// 遍历查找所有使用@RequestMapping注解的方法
				for (Method method : methods) {
					if (!method.isAnnotationPresent(RequestMapping.class)) {
						continue;
					}
					RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
					// 获取@RequestMapping注解中的映射url
					String mappingURL = requestMapping.value();
					if (!mappingURL.startsWith("/")) {
						mappingURL = "/".concat(mappingURL);
					}
					// 请求的url
					String realURL = url.append(mappingURL).toString();
					// 请求参数名和索引值的集合
					Map<String, Integer> paramMap = new HashMap<>();
					// 获取方法中所有的参数的注解，有几个参数就有几个annotation[](为毛是数组呢, 因为一个参数可以有多个注解)
					Annotation[][] annotations = method.getParameterAnnotations();
					// 获取所有参数的类型，提取Request和Response的索引  
					Class<?>[] paramTypes = method.getParameterTypes();
	                // 获取方法的参数名称, 如Controller的add方法，将得到如下数组["name", "addr", "request", "response"]
	                String[] paramNames = getMethodParamNamesByASM(clazz, method);
					if (annotations != null && annotations.length > 0) {
						// 遍历每个参数上的所有注解
						for (int i = 0; i < annotations.length; i++) {
							// annotationArray是当前方法的参数的注解数组
							Annotation[] annotationArray = annotations[i];
							// 当前参数没有使用到注解
							if (annotationArray.length == 0) {
								// 获取当前参数的类型
								Class<?> type = paramTypes[i];
								// 如果是Request或者Response，就直接用类名作key；如果是普通属性，就用属性名
								if (type == HttpServletRequest.class 
									|| type == HttpServletResponse.class) {
									paramMap.put(type.getName(), i);
								} else {
									// 参数没写@RequestParam注解，只写了String name，
									// 那么通过java是无法获取到name这个属性名的  
		                            // 通过上面asm获取的paramNames来映射
									paramMap.put(paramNames[i], i);
								}								
								continue;
							}
							// 有注解，就遍历当前参数上的所有注解
							for (int j = 0; j < annotationArray.length; j++) {
								Annotation annotation = annotationArray[j];
								// 查找@RequestParam注解
								if (annotation.annotationType() == RequestParam.class) {
									//也就是@RequestParam("name")上的"name"
		                            String paramName = ((RequestParam) annotation).value();
		                            // paramName不为空则直接使用, 否则使用参数名
		                            if (!"".equals(paramName.trim())) {  
		                                paramMap.put(paramName, i);  
		                            } else {
		                            	paramMap.put(paramNames[i], i);
		                            }
								}
							}
						}
					}
					// 根据controller, method, 请求参数创建映射关系对象
					HandlerModel handlerModel = new HandlerModel(bean, method, paramMap);
					// 将映射关系存入集合
					handlerMapping.put(realURL, handlerModel);
				}
			}
		}
	}
	
	// JDK自带类, 接口方法和抽象方法无法使用这种方式获取参数名
	private String[] getMethodParamNamesByASM(Class<?> clazz, Method method) throws IOException {
		String methodName = method.getName();
        Class<?>[] methodParameterTypes = method.getParameterTypes();
        int methodParameterCount = methodParameterTypes.length;
        String className = method.getDeclaringClass().getName();
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        String[] methodParametersNames = new String[methodParameterCount];
        ClassReader cr = new ClassReader(className);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassAdapter(cw) {
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                final Type[] argTypes = Type.getArgumentTypes(desc);
                // 参数类型不一致
                if (!methodName.equals(name) || !matchTypes(argTypes, methodParameterTypes)) {
                    return mv;
                }
                return new MethodAdapter(mv) {
                    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                        // 如果是静态方法，第一个参数就是方法参数，非静态方法，则第一个参数是 this ,然后才是方法的参数
                        int methodParameterIndex = isStatic ? index : index - 1;
                        if (0 <= methodParameterIndex && methodParameterIndex < methodParameterCount) {
                            methodParametersNames[methodParameterIndex] = name;
                        }
                        super.visitLocalVariable(name, desc, signature, start, end, index);
                    }
                };
            }
			private boolean matchTypes(Type[] types, Class<?>[] parameterTypes) {
				if (types.length != parameterTypes.length) {
		            return false;
		        }
		        for (int i = 0; i < types.length; i++) {
		            if (!Type.getType(parameterTypes[i]).equals(types[i])) {
		                return false;
		            }
		        }
		        return true;
			}
        }, 0);
        return methodParametersNames;
	}

	// 执行handler执行请求
	private void invokeHandler(HttpServletRequest request, HttpServletResponse response) {
		String requestURL = request.getRequestURL().toString();
		HandlerModel handlerModel = handlerMapping.get(requestURL);
		if (handlerModel != null) {
			
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		this.doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		invokeHandler(request, response);
	}

	@Override
	public void destroy() {}

	public class HandlerModel {

		// url对应的请求的controller对象
		private Object handler;

		// url对应的请求的方法
		private Method method;

		// 方法的参数名和索引值
		private Map<String, Integer> paramMap;

		public HandlerModel() {}

		public HandlerModel(Object handler, Method method, Map<String, Integer> paramMap) {
			this.handler = handler;
			this.method = method;
			this.paramMap = paramMap;
		}

		public Object getHandler() {
			return handler;
		}

		public void setHandler(Object handler) {
			this.handler = handler;
		}

		public Method getMethod() {
			return method;
		}

		public void setMethod(Method method) {
			this.method = method;
		}

		public Map<String, Integer> getParamMap() {
			return paramMap;
		}

		public void setParamMap(Map<String, Integer> paramMap) {
			this.paramMap = paramMap;
		}

		@Override
		public String toString() {
			return "HandlerModel [handler=" + handler + ", method=" + method + ", paramMap=" + paramMap + "]";
		}

	}
	
}
