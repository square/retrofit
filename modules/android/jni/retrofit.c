#include <string.h>
#include <jni.h>
#include <errno.h>
#include <fcntl.h>

static void throwException(JNIEnv* env, const char* typeName,
    const char* message) {
  (*env)->ThrowNew(env, (*env)->FindClass(env, typeName), message);
}

void Java_retrofit_io_Native_sync(JNIEnv* env, jclass javaType,
    jstring javaPath) {
  const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
  // assert path != NULL

  // Returns an error if path doesn't reference a directory.
  int fd = open(path, O_RDONLY | O_DIRECTORY);

  (*env)->ReleaseStringUTFChars(env, javaPath, path);

  if (fd == -1) goto io_error;
  if (fsync(fd) == -1) goto io_error;
  if (close(fd) == -1) goto io_error;

  return;

io_error:
  throwException(env, "java/io/IOException", strerror(errno));
  return;
}
