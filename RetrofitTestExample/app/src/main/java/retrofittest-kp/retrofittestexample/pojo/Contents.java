package retrofittest-kp.retrofittestexample.pojo;

/**
 * @author rebeccafranks
 * @since 15/10/23.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "quotes"
})
public class Contents {

    @JsonProperty("quotes")
    private List<Quote> quotes = new ArrayList<>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     *
     * @return
     * The quotes
     */
    @JsonProperty("quotes")
    public List<Quote> getQuotes() {
        return quotes;
    }

    /**
     *
     * @param quotes
     * The quotes
     */
    @JsonProperty("quotes")
    public void setQuotes(List<Quote> quotes) {
        this.quotes = quotes;
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
