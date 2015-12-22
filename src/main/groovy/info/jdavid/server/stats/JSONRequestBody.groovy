package info.jdavid.server.stats

import com.squareup.okhttp.MediaType
import com.squareup.okhttp.RequestBody
import info.jdavid.ok.json.Builder
import okio.Buffer
import okio.BufferedSink


public class JSONRequestBody extends RequestBody {
  private final Buffer buffer;
  private static final MediaType jsonMediaType = MediaType.parse('application/json')

  public JSONRequestBody(List<?> json) { Builder.build(buffer = new Buffer(), json) }
  public JSONRequestBody(Map<String, ?> json) { Builder.build(buffer = new Buffer(), json) }
  public JSONRequestBody(Buffer buffer) { this.buffer = buffer }

  @Override long contentLength() throws IOException { return buffer.size() }
  @Override MediaType contentType() { return jsonMediaType }
  @Override void writeTo(BufferedSink sink) throws IOException {
    sink.write(buffer, buffer.size())
  }
}
