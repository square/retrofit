package retrofittest-kp.retrofittestexample.pojo;

/**
 * @author rebeccafranks
 * @since 15/10/23.
 */


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "success",
        "contents"
})
public class QuoteOfTheDayResponse {

    @JsonProperty("success")
    private Success success;
    @JsonProperty("contents")
    private Contents contents;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * @return The success
     */
    @JsonProperty("success")
    public Success getSuccess() {
        return success;
    }

    /**
     * @param success The success
     */
    @JsonProperty("success")
    public void setSuccess(Success success) {
        this.success = success;
    }

    /**
     * @return The contents
     */
    @JsonProperty("contents")
    public Contents getContents() {
        return contents;
    }

    /**
     * @param contents The contents
     */
    @JsonProperty("contents")
    public void setContents(Contents contents) {
        this.contents = contents;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
