package com.limehee.jsonrpc.sample;

import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcParam;
import org.springframework.stereotype.Service;

@Service
public class GreetingRpcService {

    @JsonRpcMethod("ping")
    public String ping() {
        return "pong";
    }

    @JsonRpcMethod("greet")
    public String greet(GreetParams params) {
        return "hello " + params.name();
    }

    @JsonRpcMethod("sum")
    public int sum(@JsonRpcParam("left") int left, @JsonRpcParam("right") int right) {
        return left + right;
    }

    public record GreetParams(String name) {
    }
}
