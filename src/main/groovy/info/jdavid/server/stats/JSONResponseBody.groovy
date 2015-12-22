package info.jdavid.server.stats

import com.squareup.okhttp.MediaType
import com.squareup.okhttp.ResponseBody
import info.jdavid.ok.json.Builder
import okio.Buffer
import okio.BufferedSource

public class JSONResponseBody extends ResponseBody {
  private final Buffer buffer;
  private static final MediaType jsonMediaType = MediaType.parse('application/json')

  public JSONResponseBody(List<?> json) { Builder.build(buffer = new Buffer(), json) }
  public JSONResponseBody(Map<String, ?> json) { Builder.build(buffer = new Buffer(), json) }
  public JSONResponseBody(Buffer buffer) { this.buffer = buffer }
  public JSONResponseBody(BufferedSource source) { source.readAll(buffer = new Buffer()) }

  @Override long contentLength() throws IOException { return buffer.size() }
  @Override MediaType contentType() { return jsonMediaType }
  @Override BufferedSource source() { return buffer }
}
