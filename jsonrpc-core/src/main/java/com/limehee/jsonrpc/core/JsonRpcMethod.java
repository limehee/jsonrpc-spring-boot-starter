package com.limehee.jsonrpc.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java method for automatic JSON-RPC registration.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRpcMethod {

    /**
     * Optional JSON-RPC method name.
     * <p>
     * When blank, the Java method name is used.
     *
     * @return explicit JSON-RPC method name
     */
    String value() default "";
}
