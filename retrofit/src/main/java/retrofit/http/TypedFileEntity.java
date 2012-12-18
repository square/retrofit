package retrofit.http;

import org.apache.http.entity.FileEntity;
import retrofit.io.TypedFile;

/** Adapts a {@link TypedFile} to an {@link org.apache.http.HttpEntity HttpEntity}. */
class TypedFileEntity extends FileEntity {
  public TypedFileEntity(TypedFile typedFile) {
    super(typedFile.file(), typedFile.mimeType().mimeName());
  }
}
