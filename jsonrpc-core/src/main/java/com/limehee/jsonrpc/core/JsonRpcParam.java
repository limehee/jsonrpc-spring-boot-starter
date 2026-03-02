package com.limehee.jsonrpc.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the parameter name used for named-params binding.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRpcParam {

    /**
     * Parameter key inside JSON-RPC named {@code params} object.
     *
     * @return expected key name
     */
    String value();
}
