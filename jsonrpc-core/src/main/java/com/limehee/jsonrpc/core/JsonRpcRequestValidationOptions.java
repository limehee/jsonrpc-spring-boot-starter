package com.limehee.jsonrpc.core;

import java.util.Objects;

/**
 * Fine-grained validation switches for incoming JSON-RPC request validation.
 */
public final class JsonRpcRequestValidationOptions {

    private final boolean requireJsonRpcVersion20;
    private final boolean requireIdMember;
    private final boolean allowNullId;
    private final boolean allowStringId;
    private final boolean allowNumericId;
    private final boolean allowFractionalId;
    private final boolean rejectResponseFields;
    private final JsonRpcParamsTypeViolationCodePolicy paramsTypeViolationCodePolicy;

    private JsonRpcRequestValidationOptions(Builder builder) {
        this.requireJsonRpcVersion20 = builder.requireJsonRpcVersion20;
        this.requireIdMember = builder.requireIdMember;
        this.allowNullId = builder.allowNullId;
        this.allowStringId = builder.allowStringId;
        this.allowNumericId = builder.allowNumericId;
        this.allowFractionalId = builder.allowFractionalId;
        this.rejectResponseFields = builder.rejectResponseFields;
        this.paramsTypeViolationCodePolicy = builder.paramsTypeViolationCodePolicy;
    }

    /**
     * Returns default validation options aligned with JSON-RPC 2.0 request semantics.
     *
     * @return default request-validation options
     */
    public static JsonRpcRequestValidationOptions defaults() {
        return builder().build();
    }

    /**
     * Creates a mutable builder initialized with default values.
     *
     * @return options builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return whether {@code jsonrpc=="2.0"} is required
     */
    public boolean requireJsonRpcVersion20() {
        return requireJsonRpcVersion20;
    }

    /**
     * @return whether the {@code id} member must exist on requests
     */
    public boolean requireIdMember() {
        return requireIdMember;
    }

    /**
     * @return whether {@code id:null} is allowed
     */
    public boolean allowNullId() {
        return allowNullId;
    }

    /**
     * @return whether string IDs are allowed
     */
    public boolean allowStringId() {
        return allowStringId;
    }

    /**
     * @return whether numeric IDs are allowed
     */
    public boolean allowNumericId() {
        return allowNumericId;
    }

    /**
     * @return whether fractional numeric IDs are allowed
     */
    public boolean allowFractionalId() {
        return allowFractionalId;
    }

    /**
     * @return whether requests containing response-only fields ({@code result}/{@code error}) are rejected
     */
    public boolean rejectResponseFields() {
        return rejectResponseFields;
    }

    /**
     * @return error-code policy used when {@code params} exists but is neither object nor array
     */
    public JsonRpcParamsTypeViolationCodePolicy paramsTypeViolationCodePolicy() {
        return paramsTypeViolationCodePolicy;
    }

    /**
     * Builder for request validation options.
     */
    public static final class Builder {

        private boolean requireJsonRpcVersion20 = true;
        private boolean requireIdMember = false;
        private boolean allowNullId = true;
        private boolean allowStringId = true;
        private boolean allowNumericId = true;
        private boolean allowFractionalId = true;
        private boolean rejectResponseFields = false;
        private JsonRpcParamsTypeViolationCodePolicy paramsTypeViolationCodePolicy =
            JsonRpcParamsTypeViolationCodePolicy.INVALID_PARAMS;

        private Builder() {
        }

        /**
         * Enables or disables strict validation of the {@code jsonrpc} version field.
         *
         * @param enabled {@code true} to require {@code "2.0"}
         * @return this builder
         */
        public Builder requireJsonRpcVersion20(boolean enabled) {
            this.requireJsonRpcVersion20 = enabled;
            return this;
        }

        /**
         * Enables or disables required presence of the {@code id} member.
         *
         * @param enabled {@code true} to require an {@code id} member
         * @return this builder
         */
        public Builder requireIdMember(boolean enabled) {
            this.requireIdMember = enabled;
            return this;
        }

        /**
         * Enables or disables support for explicit {@code id:null}.
         *
         * @param enabled {@code true} to accept null IDs
         * @return this builder
         */
        public Builder allowNullId(boolean enabled) {
            this.allowNullId = enabled;
            return this;
        }

        /**
         * Enables or disables support for textual IDs.
         *
         * @param enabled {@code true} to accept string IDs
         * @return this builder
         */
        public Builder allowStringId(boolean enabled) {
            this.allowStringId = enabled;
            return this;
        }

        /**
         * Enables or disables support for numeric IDs.
         *
         * @param enabled {@code true} to accept numeric IDs
         * @return this builder
         */
        public Builder allowNumericId(boolean enabled) {
            this.allowNumericId = enabled;
            return this;
        }

        /**
         * Enables or disables support for fractional numeric IDs.
         *
         * @param enabled {@code true} to accept fractional numbers (for example {@code 1.5})
         * @return this builder
         */
        public Builder allowFractionalId(boolean enabled) {
            this.allowFractionalId = enabled;
            return this;
        }

        /**
         * Enables or disables request rejection when response-only fields are present.
         *
         * @param enabled {@code true} to reject requests containing {@code result}/{@code error}
         * @return this builder
         */
        public Builder rejectResponseFields(boolean enabled) {
            this.rejectResponseFields = enabled;
            return this;
        }

        /**
         * Sets the error-code mapping policy used when {@code params} exists but is neither object nor array.
         *
         * @param policy params-type violation error-code policy
         * @return this builder
         */
        public Builder paramsTypeViolationCodePolicy(JsonRpcParamsTypeViolationCodePolicy policy) {
            this.paramsTypeViolationCodePolicy = Objects.requireNonNull(policy, "policy");
            return this;
        }

        /**
         * Builds immutable validation options.
         *
         * @return immutable request validation options
         */
        public JsonRpcRequestValidationOptions build() {
            return new JsonRpcRequestValidationOptions(this);
        }
    }
}
