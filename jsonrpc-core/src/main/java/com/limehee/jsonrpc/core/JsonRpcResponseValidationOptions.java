package com.limehee.jsonrpc.core;

/**
 * Fine-grained validation switches for incoming JSON-RPC response validation.
 */
public final class JsonRpcResponseValidationOptions {

    private final boolean requireJsonRpcVersion20;
    private final boolean requireResponseIdMember;
    private final boolean allowNullResponseId;
    private final boolean allowStringResponseId;
    private final boolean allowNumericResponseId;
    private final boolean allowFractionalResponseId;
    private final boolean requireExclusiveResultOrError;
    private final boolean requireErrorObjectWhenPresent;
    private final boolean requireIntegerErrorCode;
    private final boolean requireStringErrorMessage;
    private final boolean allowRequestFieldsInResponse;

    private JsonRpcResponseValidationOptions(Builder builder) {
        this.requireJsonRpcVersion20 = builder.requireJsonRpcVersion20;
        this.requireResponseIdMember = builder.requireResponseIdMember;
        this.allowNullResponseId = builder.allowNullResponseId;
        this.allowStringResponseId = builder.allowStringResponseId;
        this.allowNumericResponseId = builder.allowNumericResponseId;
        this.allowFractionalResponseId = builder.allowFractionalResponseId;
        this.requireExclusiveResultOrError = builder.requireExclusiveResultOrError;
        this.requireErrorObjectWhenPresent = builder.requireErrorObjectWhenPresent;
        this.requireIntegerErrorCode = builder.requireIntegerErrorCode;
        this.requireStringErrorMessage = builder.requireStringErrorMessage;
        this.allowRequestFieldsInResponse = builder.allowRequestFieldsInResponse;
    }

    /**
     * Returns options configured with RFC MUST rules enabled by default.
     *
     * @return default options
     */
    public static JsonRpcResponseValidationOptions defaults() {
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
     * @return whether the {@code id} member must exist
     */
    public boolean requireResponseIdMember() {
        return requireResponseIdMember;
    }

    /**
     * @return whether {@code id:null} is allowed
     */
    public boolean allowNullResponseId() {
        return allowNullResponseId;
    }

    /**
     * @return whether string IDs are allowed
     */
    public boolean allowStringResponseId() {
        return allowStringResponseId;
    }

    /**
     * @return whether numeric IDs are allowed
     */
    public boolean allowNumericResponseId() {
        return allowNumericResponseId;
    }

    /**
     * @return whether fractional numeric IDs are allowed
     */
    public boolean allowFractionalResponseId() {
        return allowFractionalResponseId;
    }

    /**
     * @return whether exactly one of {@code result}/{@code error} is required
     */
    public boolean requireExclusiveResultOrError() {
        return requireExclusiveResultOrError;
    }

    /**
     * @return whether {@code error} must be an object when present
     */
    public boolean requireErrorObjectWhenPresent() {
        return requireErrorObjectWhenPresent;
    }

    /**
     * @return whether {@code error.code} must be an integer number
     */
    public boolean requireIntegerErrorCode() {
        return requireIntegerErrorCode;
    }

    /**
     * @return whether {@code error.message} must be a string
     */
    public boolean requireStringErrorMessage() {
        return requireStringErrorMessage;
    }

    /**
     * @return whether response objects may include request-only fields such as {@code method}/{@code params}
     */
    public boolean allowRequestFieldsInResponse() {
        return allowRequestFieldsInResponse;
    }

    /**
     * Builder for response validation options.
     */
    public static final class Builder {

        private boolean requireJsonRpcVersion20 = true;
        private boolean requireResponseIdMember = true;
        private boolean allowNullResponseId = true;
        private boolean allowStringResponseId = true;
        private boolean allowNumericResponseId = true;
        private boolean allowFractionalResponseId = true;
        private boolean requireExclusiveResultOrError = true;
        private boolean requireErrorObjectWhenPresent = true;
        private boolean requireIntegerErrorCode = true;
        private boolean requireStringErrorMessage = true;
        private boolean allowRequestFieldsInResponse = true;

        private Builder() {
        }

        public Builder requireJsonRpcVersion20(boolean enabled) {
            this.requireJsonRpcVersion20 = enabled;
            return this;
        }

        public Builder requireResponseIdMember(boolean enabled) {
            this.requireResponseIdMember = enabled;
            return this;
        }

        public Builder allowNullResponseId(boolean enabled) {
            this.allowNullResponseId = enabled;
            return this;
        }

        public Builder allowStringResponseId(boolean enabled) {
            this.allowStringResponseId = enabled;
            return this;
        }

        public Builder allowNumericResponseId(boolean enabled) {
            this.allowNumericResponseId = enabled;
            return this;
        }

        public Builder allowFractionalResponseId(boolean enabled) {
            this.allowFractionalResponseId = enabled;
            return this;
        }

        public Builder requireExclusiveResultOrError(boolean enabled) {
            this.requireExclusiveResultOrError = enabled;
            return this;
        }

        public Builder requireErrorObjectWhenPresent(boolean enabled) {
            this.requireErrorObjectWhenPresent = enabled;
            return this;
        }

        public Builder requireIntegerErrorCode(boolean enabled) {
            this.requireIntegerErrorCode = enabled;
            return this;
        }

        public Builder requireStringErrorMessage(boolean enabled) {
            this.requireStringErrorMessage = enabled;
            return this;
        }

        public Builder allowRequestFieldsInResponse(boolean enabled) {
            this.allowRequestFieldsInResponse = enabled;
            return this;
        }

        public JsonRpcResponseValidationOptions build() {
            return new JsonRpcResponseValidationOptions(this);
        }
    }
}
