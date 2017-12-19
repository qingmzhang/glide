package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class VideoDecoderTest {
  @Mock private ParcelFileDescriptor resource;
  @Mock private VideoDecoder.MediaMetadataRetrieverFactory factory;
  @Mock private VideoDecoder.MediaMetadataRetrieverInitializer<ParcelFileDescriptor> initializer;
  @Mock private MediaMetadataRetriever retriever;
  @Mock private BitmapPool bitmapPool;
  private VideoDecoder<ParcelFileDescriptor> decoder;
  private Options options;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(factory.build()).thenReturn(retriever);
    decoder = new VideoDecoder<>(bitmapPool, initializer, factory);
    options = new Options();
  }

  @Test
  public void testReturnsRetrievedFrameForResource() throws IOException {
    Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(retriever.getFrameAtTime()).thenReturn(expected);

    Resource<Bitmap> result =
        Preconditions.checkNotNull(decoder.decode(resource, 100, 100, options));

    verify(initializer).initialize(retriever, resource);
    assertEquals(expected, result.get());
  }

  @Test
  public void testReleasesMediaMetadataRetriever() throws IOException {
    decoder.decode(resource, 1, 2, options);

    verify(retriever).release();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsExceptionIfCalledWithInvalidFrame() throws IOException {
    options.set(VideoDecoder.TARGET_FRAME, -5L);
    new VideoDecoder<>(bitmapPool, initializer, factory).decode(resource, 100, 100, options);
  }

  @Test
  public void testSpecifiesThumbnailFrameIfICalledWithFrameNumber() throws IOException {
    long frame = 5;
    options.set(VideoDecoder.TARGET_FRAME, frame);
    decoder = new VideoDecoder<>(bitmapPool, initializer, factory);

    decoder.decode(resource, 100, 100, options);

    verify(retriever).getFrameAtTime(frame);
    verify(retriever, never()).getFrameAtTime();
  }

  @Test
  public void testDoesNotSpecifyThumbnailFrameIfCalledWithoutFrameNumber() throws IOException {
    decoder = new VideoDecoder<>(bitmapPool, initializer, factory);
    decoder.decode(resource, 100, 100, options);

    verify(retriever).getFrameAtTime();
    verify(retriever, never()).getFrameAtTime(anyLong());
  }
}