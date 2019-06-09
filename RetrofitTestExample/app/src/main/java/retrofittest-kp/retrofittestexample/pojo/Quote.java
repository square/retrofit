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
        "quote",
        "length",
        "author",
        "tags",
        "category",
        "id"
})
public class Quote {

    @JsonProperty("quote")
    private String quote;
    @JsonProperty("length")
    private String length;
    @JsonProperty("author")
    private String author;
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();
    @JsonProperty("category")
    private String category;
    @JsonProperty("id")
    private String id;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     *
     * @return
     * The quote
     */
    @JsonProperty("quote")
    public String getQuote() {
        return quote;
    }

    /**
     *
     * @param quote
     * The quote
     */
    @JsonProperty("quote")
    public void setQuote(String quote) {
        this.quote = quote;
    }

    /**
     *
     * @return
     * The length
     */
    @JsonProperty("length")
    public String getLength() {
        return length;
    }

    /**
     *
     * @param length
     * The length
     */
    @JsonProperty("length")
    public void setLength(String length) {
        this.length = length;
    }

    /**
     *
     * @return
     * The author
     */
    @JsonProperty("author")
    public String getAuthor() {
        return author;
    }

    /**
     *
     * @param author
     * The author
     */
    @JsonProperty("author")
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     *
     * @return
     * The tags
     */
    @JsonProperty("tags")
    public List<String> getTags() {
        return tags;
    }

    /**
     *
     * @param tags
     * The tags
     */
    @JsonProperty("tags")
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     *
     * @return
     * The category
     */
    @JsonProperty("category")
    public String getCategory() {
        return category;
    }

    /**
     *
     * @param category
     * The category
     */
    @JsonProperty("category")
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     *
     * @return
     * The id
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     *
     * @param id
     * The id
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
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
