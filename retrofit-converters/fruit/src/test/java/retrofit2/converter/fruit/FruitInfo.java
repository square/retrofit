package retrofit2.converter.fruit;

import me.ghui.fruit.Attrs;
import me.ghui.fruit.annotations.Pick;

import java.util.List;

public class FruitInfo {

  @Pick("title")
  private String title;
  @Pick("div#favorite")
  private String favoriteOne;
  @Pick(value = "img.apple", attr = Attrs.SRC)
  private String favoriteImg;
  @Pick("a.author")
  private String author;
  @Pick(value = "a.author", attr = Attrs.HREF)
  private String authorBlog;

  @Pick("div#fruits div.fruit")
  private List<Item> otherFruits;

  public FruitInfo() {
  }

  public String getTitle() {
    return title;
  }

  public String getFavoriteOne() {
    return favoriteOne;
  }

  public String getFavoriteImg() {
    return favoriteImg;
  }

  public String getAuthor() {
    return author;
  }

  public String getAuthorBlog() {
    return authorBlog;
  }

  public List<Item> getOtherFruits() {
    return otherFruits;
  }

  public static class Item {
    @Pick("strong.name")
    private String name;
    @Pick(".color")
    private String color;
    @Pick(attr = "id")
    private int id;

    public int getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getColor() {
      return color;
    }
  }

}
