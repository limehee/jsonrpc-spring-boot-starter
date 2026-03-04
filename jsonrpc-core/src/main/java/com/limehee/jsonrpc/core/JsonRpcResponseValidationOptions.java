package com.limehee.jsonrpc.core;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Fine-grained validation switches for incoming JSON-RPC response validation.
 */
public final class JsonRpcResponseValidationOptions {

    private final boolean requireJsonRpcVersion20;
    private final boolean requireIdMember;
    private final boolean allowNullId;
    private final boolean allowStringId;
    private final boolean allowNumericId;
    private final boolean allowFractionalId;
    private final boolean requireExclusiveResultOrError;
    private final boolean requireErrorObjectWhenPresent;
    private final boolean requireIntegerErrorCode;
    private final boolean requireStringErrorMessage;
    private final boolean rejectRequestFields;
    private final boolean rejectDuplicateMembers;
    private final JsonRpcResponseErrorCodePolicy errorCodePolicy;
    private final @Nullable Integer errorCodeRangeMin;
    private final @Nullable Integer errorCodeRangeMax;

    private JsonRpcResponseValidationOptions(Builder builder) {
        this.requireJsonRpcVersion20 = builder.requireJsonRpcVersion20;
        this.requireIdMember = builder.requireIdMember;
        this.allowNullId = builder.allowNullId;
        this.allowStringId = builder.allowStringId;
        this.allowNumericId = builder.allowNumericId;
        this.allowFractionalId = builder.allowFractionalId;
        this.requireExclusiveResultOrError = builder.requireExclusiveResultOrError;
        this.requireErrorObjectWhenPresent = builder.requireErrorObjectWhenPresent;
        this.requireIntegerErrorCode = builder.requireIntegerErrorCode;
        this.requireStringErrorMessage = builder.requireStringErrorMessage;
        this.rejectRequestFields = builder.rejectRequestFields;
        this.rejectDuplicateMembers = builder.rejectDuplicateMembers;
        this.errorCodePolicy = builder.errorCodePolicy;
        this.errorCodeRangeMin = builder.errorCodeRangeMin;
        this.errorCodeRangeMax = builder.errorCodeRangeMax;
    }

    /**
     * Returns default validation options.
     * <p>
     * RFC MUST rules are enabled by default. Optional interoperability-related rules stay permissive unless explicitly
     * restricted via builder switches.
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
     * @return whether response objects containing request-only fields ({@code method}/{@code params}) are rejected
     */
    public boolean rejectRequestFields() {
        return rejectRequestFields;
    }

    /**
     * @return whether response duplicate object members should be rejected during raw payload parsing
     */
    public boolean rejectDuplicateMembers() {
        return rejectDuplicateMembers;
    }

    /**
     * @return policy restricting accepted {@code error.code} integers
     */
    public JsonRpcResponseErrorCodePolicy errorCodePolicy() {
        return errorCodePolicy;
    }

    /**
     * @return lower bound for {@code error.code} when {@link #errorCodePolicy()} is {@code CUSTOM_RANGE}
     */
    public @Nullable Integer errorCodeRangeMin() {
        return errorCodeRangeMin;
    }

    /**
     * @return upper bound for {@code error.code} when {@link #errorCodePolicy()} is {@code CUSTOM_RANGE}
     */
    public @Nullable Integer errorCodeRangeMax() {
        return errorCodeRangeMax;
    }

    /**
     * Builder for response validation options.
     */
    public static final class Builder {

        private boolean requireJsonRpcVersion20 = true;
        private boolean requireIdMember = true;
        private boolean allowNullId = true;
        private boolean allowStringId = true;
        private boolean allowNumericId = true;
        private boolean allowFractionalId = true;
        private boolean requireExclusiveResultOrError = true;
        private boolean requireErrorObjectWhenPresent = true;
        private boolean requireIntegerErrorCode = true;
        private boolean requireStringErrorMessage = true;
        private boolean rejectRequestFields = false;
        private boolean rejectDuplicateMembers = false;
        private JsonRpcResponseErrorCodePolicy errorCodePolicy = JsonRpcResponseErrorCodePolicy.ANY_INTEGER;
        private @Nullable Integer errorCodeRangeMin;
        private @Nullable Integer errorCodeRangeMax;

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
         * Enables or disables support for textual response IDs.
         *
         * @param enabled {@code true} to accept string IDs
         * @return this builder
         */
        public Builder allowStringId(boolean enabled) {
            this.allowStringId = enabled;
            return this;
        }

        /**
         * Enables or disables support for numeric response IDs.
         *
         * @param enabled {@code true} to accept numeric IDs
         * @return this builder
         */
        public Builder allowNumericId(boolean enabled) {
            this.allowNumericId = enabled;
            return this;
        }

        /**
         * Enables or disables support for fractional numeric response IDs.
         *
         * @param enabled {@code true} to accept fractional numbers (for example {@code 1.5})
         * @return this builder
         */
        public Builder allowFractionalId(boolean enabled) {
            this.allowFractionalId = enabled;
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
         * Enables or disables rejection for request-only fields in response objects.
         *
         * @param enabled {@code true} to reject response objects containing {@code method}/{@code params}
         * @return this builder
         */
        public Builder rejectRequestFields(boolean enabled) {
            this.rejectRequestFields = enabled;
            return this;
        }

        /**
         * Enables or disables duplicate member rejection in response payload parsing.
         *
         * @param enabled {@code true} to reject duplicate members while parsing raw response JSON
         * @return this builder
         */
        public Builder rejectDuplicateMembers(boolean enabled) {
            this.rejectDuplicateMembers = enabled;
            return this;
        }

        /**
         * Sets the accepted range policy for integer response {@code error.code} values.
         *
         * @param policy error-code policy
         * @return this builder
         */
        public Builder errorCodePolicy(JsonRpcResponseErrorCodePolicy policy) {
            this.errorCodePolicy = Objects.requireNonNull(policy, "policy");
            return this;
        }

        /**
         * Sets the lower bound for {@code error.code} when using {@code CUSTOM_RANGE}.
         *
         * @param min inclusive lower bound
         * @return this builder
         */
        public Builder errorCodeRangeMin(@Nullable Integer min) {
            this.errorCodeRangeMin = min;
            return this;
        }

        /**
         * Sets the upper bound for {@code error.code} when using {@code CUSTOM_RANGE}.
         *
         * @param max inclusive upper bound
         * @return this builder
         */
        public Builder errorCodeRangeMax(@Nullable Integer max) {
            this.errorCodeRangeMax = max;
            return this;
        }

        /**
         * Builds immutable validation options.
         *
         * @return immutable response validation options
         */
        public JsonRpcResponseValidationOptions build() {
            if (!requireIntegerErrorCode && errorCodePolicy != JsonRpcResponseErrorCodePolicy.ANY_INTEGER) {
                throw new IllegalArgumentException(
                    "errorCodePolicy requires requireIntegerErrorCode=true when policy is not ANY_INTEGER"
                );
            }
            if (errorCodePolicy == JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE) {
                if (errorCodeRangeMin == null || errorCodeRangeMax == null) {
                    throw new IllegalArgumentException(
                        "CUSTOM_RANGE requires both errorCodeRangeMin and errorCodeRangeMax"
                    );
                }
                if (errorCodeRangeMin > errorCodeRangeMax) {
                    throw new IllegalArgumentException("errorCodeRangeMin must be less than or equal to errorCodeRangeMax");
                }
            }
            return new JsonRpcResponseValidationOptions(this);
        }
    }
}
