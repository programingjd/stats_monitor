package info.jdavid.server.stats

import okhttp3.MediaType
import okhttp3.RequestBody
import info.jdavid.ok.json.Builder
import info.jdavid.ok.server.MediaTypes
import okio.Buffer
import okio.BufferedSink


public class JSONRequestBody extends RequestBody {
  private final Buffer buffer;
  private static final MediaType jsonMediaType = MediaTypes.JSON

  public JSONRequestBody(List<?> json) { Builder.build(buffer = new Buffer(), json) }
  public JSONRequestBody(Map<String, ?> json) { Builder.build(buffer = new Buffer(), json) }
  public JSONRequestBody(Buffer buffer) { this.buffer = buffer }

  @Override long contentLength() throws IOException { return buffer.size() }
  @Override MediaType contentType() { return jsonMediaType }
  @Override void writeTo(BufferedSink sink) throws IOException {
    sink.write(buffer, buffer.size())
  }
}
