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
     * Returns default validation options.
     * <p>
     * RFC MUST rules are enabled by default. Compatibility-related rules are also configured with permissive defaults
     * unless explicitly restricted through builder switches.
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
     * @return whether response objects may include request-only fields such as {@code method}/{@code params}; this is a
     * compatibility policy and not an RFC MUST rule
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
        public Builder requireResponseIdMember(boolean enabled) {
            this.requireResponseIdMember = enabled;
            return this;
        }

        /**
         * Enables or disables support for explicit {@code id:null}.
         *
         * @param enabled {@code true} to accept null IDs
         * @return this builder
         */
        public Builder allowNullResponseId(boolean enabled) {
            this.allowNullResponseId = enabled;
            return this;
        }

        /**
         * Enables or disables support for textual response IDs.
         *
         * @param enabled {@code true} to accept string IDs
         * @return this builder
         */
        public Builder allowStringResponseId(boolean enabled) {
            this.allowStringResponseId = enabled;
            return this;
        }

        /**
         * Enables or disables support for numeric response IDs.
         *
         * @param enabled {@code true} to accept numeric IDs
         * @return this builder
         */
        public Builder allowNumericResponseId(boolean enabled) {
            this.allowNumericResponseId = enabled;
            return this;
        }

        /**
         * Enables or disables support for fractional numeric response IDs.
         *
         * @param enabled {@code true} to accept fractional numbers (for example {@code 1.5})
         * @return this builder
         */
        public Builder allowFractionalResponseId(boolean enabled) {
            this.allowFractionalResponseId = enabled;
            return this;
        }

        /**
         * Enables or disables strict exclusivity between {@code result} and {@code error}.
         *
         * @param enabled {@code true} to require exactly one of the two members
         * @return this builder
         */
        public Builder requireExclusiveResultOrError(boolean enabled) {
            this.requireExclusiveResultOrError = enabled;
            return this;
        }

        /**
         * Enables or disables object-shape enforcement for the {@code error} member.
         *
         * @param enabled {@code true} to require {@code error} to be an object when present
         * @return this builder
         */
        public Builder requireErrorObjectWhenPresent(boolean enabled) {
            this.requireErrorObjectWhenPresent = enabled;
            return this;
        }

        /**
         * Enables or disables integer enforcement for {@code error.code}.
         *
         * @param enabled {@code true} to require an integer code
         * @return this builder
         */
        public Builder requireIntegerErrorCode(boolean enabled) {
            this.requireIntegerErrorCode = enabled;
            return this;
        }

        /**
         * Enables or disables string enforcement for {@code error.message}.
         *
         * @param enabled {@code true} to require a string message
         * @return this builder
         */
        public Builder requireStringErrorMessage(boolean enabled) {
            this.requireStringErrorMessage = enabled;
            return this;
        }

        /**
         * Enables or disables tolerance for request-only fields in response objects.
         *
         * @param enabled {@code true} to allow response objects containing {@code method}/{@code params}
         * @return this builder
         */
        public Builder allowRequestFieldsInResponse(boolean enabled) {
            this.allowRequestFieldsInResponse = enabled;
            return this;
        }

        /**
         * Builds immutable validation options.
         *
         * @return immutable response validation options
         */
        public JsonRpcResponseValidationOptions build() {
            return new JsonRpcResponseValidationOptions(this);
        }
    }
}
