package se.lnu.siq.s4rdm3x;

import java.lang.reflect.Method;

// Helper class to allow calling private methods in any class.
public class MagicInvoker {
    private Object m_caller;    // we use a member as it is too easy to forget to add this when calling invokeMethodMagic as it takes an Object ...

    public MagicInvoker(Object a_caller) {
        m_caller = a_caller;
    }

    // tries to find a method by searching a class hierarchy recursively upwards
    public Method getMethodRecurse(Class a_class, String a_name,  Class<?>... a_parameterTypes) {
        Method[] methods = a_class.getDeclaredMethods();
        for (int mIx = 0; mIx < methods.length; mIx++) {
            if (methods[mIx].getName().equals(a_name)) {
                Class[] params = methods[mIx].getParameterTypes();
                if (params.length == a_parameterTypes.length) {
                    int pIx = 0;
                    while(pIx < params.length && params[pIx].isAssignableFrom(a_parameterTypes[pIx])) {
                        pIx++;
                    }
                    if (pIx == params.length) {
                        return methods[mIx];
                    }
                }
            }
        }
        // try the parent
        if (a_class.getSuperclass() != null) {
            return getMethodRecurse(a_class.getSuperclass(), a_name, a_parameterTypes);
        } else {
            return null;
        }
    }

    public Method findMethod(String a_methodName, Object ... a_args) {
        Class caller = m_caller.getClass();

        Class<?> [] classes = new Class<?>[a_args.length];
        for (int i = 0; i < a_args.length; i++) {
            classes[i] = a_args[i].getClass();
        }

        return getMethodRecurse(caller.getSuperclass(), a_methodName, classes);
    }

    // Invokes a named method with arguments
    public Object invokeMethod(String a_methodName, Object ... a_args) {
        try {
            Method sutMethod = findMethod(a_methodName, a_args);
            sutMethod.setAccessible(true);
            return sutMethod.invoke(m_caller, a_args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // uses the caller method name anc class to invoke a method, i.e. create an inner class and inherit from the wanted class,
    // and override the intended method, the outer class can then call the privateMethod (as outer classes have access to inner class privates)
    //
    // class SomeInnerClass extends HasPrivateMethod {
    //
    //   private String thePrivateMethod(String a_arg) {
    //      MagicInvoker mi = new MagicInvoker(this);
    //      return (String)mi.invokeMethodMagic(a_arg);
    //   }
    // }
    //
    public Object invokeMethodMagic(Object... a_args) {
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        return invokeMethod(methodName, a_args);
    }
}
