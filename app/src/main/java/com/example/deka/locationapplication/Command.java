package com.example.deka.locationapplication;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Deka on 15/09/2016.
 */
public class Command {

    private Object receiver;
    private Method action;
    private Object[] args;                   // the "pre-registered" arg list

    public Command(Object obj, String methodName, Object[] arguments) throws NoSuchMethodException {
        receiver = obj;
        args = arguments;

        //Fetch args
        Class[] argTypes = null;
        if(null != args) {
            argTypes = new Class[args.length];

            for (int i = 0; i < args.length; i++)   // get the "Class" for each
                argTypes[i] = args[i].getClass();  //    supplied argument
        }

        // get the "Method" data structure with the correct name and signature
        try {
            action = obj.getClass().getDeclaredMethod(methodName, argTypes);
        } catch (NoSuchMethodException e) {
            throw e;
        }
    }

    public Command(Object obj, String methodName) throws NoSuchMethodException {
        this(obj, methodName, null);
    }

    private Command(Object obj, Method action, Object[] arguments){
        receiver= obj;
        this.action  = action;
        args    = arguments;

    }

    public static Command create(Object obj, String methodName, Object[] args) throws NoSuchMethodException {
        return new Command(obj, methodName, args);
    }

    public Object execute() {
        try {
            action.setAccessible(true);
            return action.invoke(receiver, args);
        } catch (IllegalAccessException e) {
            System.out.println(e);
        } catch (InvocationTargetException e) {
            System.out.println(e);
        }

        return null;
    }

    public String toString(){
        //
        String args_str = "{";
        for (Object arg: args) {
            args_str += arg.toString() + ",";
        }
        args_str += "}";

        //
        return "{Class: "+receiver.getClass().getSimpleName()
                + ", Method: "+ action.getName()
                + ", Arguments: "+ args_str
                +"}";
    }
}
