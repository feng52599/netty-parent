/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.handler.ssl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;
import javax.xml.bind.DatatypeConverter;

import org.junit.Test;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.DomainNameMapping;
import io.netty.util.DomainNameMappingBuilder;
import io.netty.util.Mapping;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;

public class SniHandlerTest {

    private static ApplicationProtocolConfig newApnConfig() {
        return new ApplicationProtocolConfig(
                ApplicationProtocolConfig.Protocol.ALPN,
                // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                "myprotocol");
    }

    private static SslContext makeSslContext() throws Exception {
        return makeSslContext(null);
    }

    private static SslContext makeSslContext(SslProvider provider) throws Exception {
        File keyFile = new File(SniHandlerTest.class.getResource("test_encrypted.pem").getFile());
        File crtFile = new File(SniHandlerTest.class.getResource("test.crt").getFile());

        return SslContextBuilder.forServer(crtFile, keyFile, "12345")
                .sslProvider(provider)
                .applicationProtocolConfig(newApnConfig()).build();
    }

    private static SslContext makeSslClientContext() throws Exception {
        File crtFile = new File(SniHandlerTest.class.getResource("test.crt").getFile());

        return SslContextBuilder.forClient().trustManager(crtFile).applicationProtocolConfig(newApnConfig()).build();
    }

    @Test
    public void testServerNameParsing() throws Exception {
        SslContext nettyContext = makeSslContext();
        SslContext leanContext = makeSslContext();
        SslContext leanContext2 = makeSslContext();

        DomainNameMapping<SslContext> mapping = new DomainNameMappingBuilder<SslContext>(nettyContext)
                .add("*.netty.io", nettyContext)
                // input with custom cases
                .add("*.LEANCLOUD.CN", leanContext)
                // a hostname conflict with previous one, since we are using order-sensitive config, the engine won't
                // be used with the handler.
                .add("chat4.leancloud.cn", leanContext2)
                .build();

        SniHandler handler = new SniHandler(mapping);
        EmbeddedChannel ch = new EmbeddedChannel(handler);

        // hex dump of a client hello packet, which contains hostname "CHAT4???LEANCLOUD???CN"
        String tlsHandshakeMessageHex1 = "16030100";
        // part 2
        String tlsHandshakeMessageHex = "bd010000b90303a74225676d1814ba57faff3b366" +
                "3656ed05ee9dbb2a4dbb1bb1c32d2ea5fc39e0000000100008c0000001700150000164348" +
                "415434E380824C45414E434C4F5544E38082434E000b000403000102000a00340032000e0" +
                "00d0019000b000c00180009000a0016001700080006000700140015000400050012001300" +
                "0100020003000f0010001100230000000d0020001e0601060206030501050205030401040" +
                "20403030103020303020102020203000f00010133740000";

        try {
            // Push the handshake message.
            // Decode should fail because SNI error
            ch.writeInbound(Unpooled.wrappedBuffer(DatatypeConverter.parseHexBinary(tlsHandshakeMessageHex1)));
            ch.writeInbound(Unpooled.wrappedBuffer(DatatypeConverter.parseHexBinary(tlsHandshakeMessageHex)));
            fail();
        } catch (DecoderException e) {
            // expected
        }

        assertThat(ch.finish(), is(true));
        assertThat(handler.hostname(), is("chat4.leancloud.cn"));
        assertThat(handler.sslContext(), is(leanContext));

        for (;;) {
            Object msg = ch.readOutbound();
            if (msg == null) {
                break;
            }
            ReferenceCountUtil.release(msg);
        }
    }

    @Test
    public void testFallbackToDefaultContext() throws Exception {
        SslContext nettyContext = makeSslContext();
        SslContext leanContext = makeSslContext();
        SslContext leanContext2 = makeSslContext();

        DomainNameMapping<SslContext> mapping = new DomainNameMappingBuilder<SslContext>(nettyContext)
                .add("*.netty.io", nettyContext)
                // input with custom cases
                .add("*.LEANCLOUD.CN", leanContext)
                // a hostname conflict with previous one, since we are using order-sensitive config, the engine won't
                // be used with the handler.
                .add("chat4.leancloud.cn", leanContext2)
                .build();

        SniHandler handler = new SniHandler(mapping);
        EmbeddedChannel ch = new EmbeddedChannel(handler);

        // invalid
        byte[] message = { 22, 3, 1, 0, 0 };

        try {
            // Push the handshake message.
            ch.writeInbound(Unpooled.wrappedBuffer(message));
        } catch (Exception e) {
            // expected
        }

        assertThat(ch.finish(), is(false));
        assertThat(handler.hostname(), nullValue());
        assertThat(handler.sslContext(), is(nettyContext));
    }

    @Test
    public void testSniWithApnHandler() throws Exception {
        SslContext nettyContext = makeSslContext();
        SslContext sniContext = makeSslContext();
        final SslContext clientContext = makeSslClientContext();
        final CountDownLatch serverApnDoneLatch = new CountDownLatch(1);
        final CountDownLatch clientApnDoneLatch = new CountDownLatch(1);

        final DomainNameMapping<SslContext> mapping = new DomainNameMappingBuilder<SslContext>(nettyContext)
                                                         .add("*.netty.io", nettyContext)
                                                         .add("sni.fake.site", sniContext).build();
        final SniHandler handler = new SniHandler(mapping);
        EventLoopGroup group = new NioEventLoopGroup(2);
        Channel serverChannel = null;
        Channel clientChannel = null;
        try {
            ServerBootstrap sb = new ServerBootstrap();
            sb.group(group);
            sb.channel(NioServerSocketChannel.class);
            sb.childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    // Server side SNI.
                    p.addLast(handler);
                    // Catch the notification event that APN has completed successfully.
                    p.addLast(new ApplicationProtocolNegotiationHandler("foo") {
                        @Override
                        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
                            serverApnDoneLatch.countDown();
                        }
                    });
                }
            });

            Bootstrap cb = new Bootstrap();
            cb.group(group);
            cb.channel(NioSocketChannel.class);
            cb.handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(new SslHandler(clientContext.newEngine(
                            ch.alloc(), "sni.fake.site", -1)));
                    // Catch the notification event that APN has completed successfully.
                    ch.pipeline().addLast(new ApplicationProtocolNegotiationHandler("foo") {
                        @Override
                        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
                            clientApnDoneLatch.countDown();
                        }
                    });
                }
            });

            serverChannel = sb.bind(new InetSocketAddress(0)).sync().channel();

            ChannelFuture ccf = cb.connect(serverChannel.localAddress());
            assertTrue(ccf.awaitUninterruptibly().isSuccess());
            clientChannel = ccf.channel();

            assertTrue(serverApnDoneLatch.await(5, TimeUnit.SECONDS));
            assertTrue(clientApnDoneLatch.await(5, TimeUnit.SECONDS));
            assertThat(handler.hostname(), is("sni.fake.site"));
            assertThat(handler.sslContext(), is(sniContext));
        } finally {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
            if (clientChannel != null) {
                clientChannel.close().sync();
            }
            group.shutdownGracefully(0, 0, TimeUnit.MICROSECONDS);
        }
    }

    @Test(timeout = 10L * 1000L)
    public void testReplaceHandler() throws Exception {

        assumeTrue(OpenSsl.isAvailable());

        final String sniHost = "sni.netty.io";
        LocalAddress address = new LocalAddress("testReplaceHandler-" + Math.random());
        EventLoopGroup group = new DefaultEventLoopGroup(1);
        Channel sc = null;
        Channel cc = null;

        SelfSignedCertificate cert = new SelfSignedCertificate();

        try {
            final SslContext sslServerContext = SslContextBuilder
                    .forServer(cert.key(), cert.cert())
                    .sslProvider(SslProvider.OPENSSL)
                    .build();

            final Mapping<String, SslContext> mapping = new Mapping<String, SslContext>() {
                @Override
                public SslContext map(String input) {
                    return sslServerContext;
                }
            };

            final Promise<Void> releasePromise = group.next().newPromise();

            final SniHandler handler = new SniHandler(mapping) {
                @Override
                protected void replaceHandler(ChannelHandlerContext ctx,
                        String hostname, final SslContext sslContext)
                        throws Exception {

                    boolean success = false;
                    try {
                        // The SniHandler's replaceHandler() method allows us to implement custom behavior.
                        // As an example, we want to release() the SslContext upon channelInactive() or rather
                        // when the SslHandler closes it's SslEngine. If you take a close look at SslHandler
                        // you'll see that it's doing it in the #handlerRemoved0() method.

                        SSLEngine sslEngine = sslContext.newEngine(ctx.alloc());
                        try {
                            SslHandler customSslHandler = new CustomSslHandler(sslContext, sslEngine) {
                                @Override
                                public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
                                    try {
                                        super.handlerRemoved0(ctx);
                                    } finally {
                                        releasePromise.trySuccess(null);
                                    }
                                }
                            };
                            ctx.pipeline().replace(this, CustomSslHandler.class.getName(), customSslHandler);
                            success = true;
                        } finally {
                            if (!success) {
                                ReferenceCountUtil.safeRelease(sslEngine);
                            }
                        }
                    } finally {
                        if (!success) {
                            ReferenceCountUtil.safeRelease(sslContext);
                            releasePromise.cancel(true);
                        }
                    }
                }
            };

            ServerBootstrap sb = new ServerBootstrap();
            sc = sb.group(group).channel(LocalServerChannel.class).childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addFirst(handler);
                }
            }).bind(address).syncUninterruptibly().channel();

            SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            Bootstrap cb = new Bootstrap();
            cc = cb.group(group).channel(LocalChannel.class).handler(new SslHandler(
                    sslContext.newEngine(ByteBufAllocator.DEFAULT, sniHost, -1)))
                    .connect(address).syncUninterruptibly().channel();

            cc.writeAndFlush(Unpooled.wrappedBuffer("Hello, World!".getBytes()))
                .syncUninterruptibly();

            // Notice how the server's SslContext refCnt is 1
            assertEquals(1, ((ReferenceCounted) sslServerContext).refCnt());

            // The client disconnects
            cc.close().syncUninterruptibly();
            if (!releasePromise.awaitUninterruptibly(10L, TimeUnit.SECONDS)) {
                throw new IllegalStateException("It doesn't seem #replaceHandler() got called.");
            }

            // We should have successfully release() the SslContext
            assertEquals(0, ((ReferenceCounted) sslServerContext).refCnt());
        } finally {
            if (cc != null) {
                cc.close().syncUninterruptibly();
            }
            if (sc != null) {
                sc.close().syncUninterruptibly();
            }
            group.shutdownGracefully();

            cert.delete();
        }
    }

    /**
     * This is a {@link SslHandler} that will call {@code release()} on the {@link SslContext} when
     * the client disconnects.
     *
     * @see SniHandlerTest#testReplaceHandler()
     */
    private static class CustomSslHandler extends SslHandler {
        private final SslContext sslContext;

        public CustomSslHandler(SslContext sslContext, SSLEngine sslEngine) {
            super(sslEngine);
            this.sslContext = ObjectUtil.checkNotNull(sslContext, "sslContext");
        }

        @Override
        public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
            super.handlerRemoved0(ctx);
            ReferenceCountUtil.release(sslContext);
        }
    }
}
