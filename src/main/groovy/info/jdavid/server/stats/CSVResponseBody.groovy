package info.jdavid.server.stats

import com.squareup.okhttp.MediaType
import com.squareup.okhttp.ResponseBody
import info.jdavid.ok.server.MediaTypes
import okio.Buffer
import okio.BufferedSource


public class CSVResponseBody extends ResponseBody {
  private final long length
  private final BufferedSource source
  private static final MediaType csvMediaType = MediaTypes.CSV

  public CSVResponseBody(final long length, final BufferedSource source) {
    this.length = length
    this.source = source
  }
  public CSVResponseBody(final String text) {
    final Buffer buffer = new Buffer()
    buffer.writeUtf8(text)
    this.length = buffer.size()
    this.source = buffer
  }

  @Override MediaType contentType() { csvMediaType }
  @Override long contentLength() { length }
  @Override BufferedSource source() { source }
}
