/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.server;

/**
 * Simple interface that represents a fully configured web server (for example Tomcat,
 * Jetty, Netty). Allows the server to be {@link #start() started} and {@link #stop()
 * stopped}.
 * <p>
 * 表示完全配置的web服务器的简单接口（例如：Tomcat、Jetty、Netty）,允许服务器start和stop
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 2.0.0
 */
public interface WebServer {

	/**
	 * Starts the web server. Calling this method on an already started server has no effect.
	 *
	 * 启动web server,在一个已经启动的服务器上调用此方法没有任何效果
	 *
	 * @throws WebServerException if the server cannot be started
	 */
	void start() throws WebServerException;

	/**
	 * Stops the web server. Calling this method on an already stopped server has no effect.
	 * 停止web server,在一个已经停止的服务器上调用此方法没有任何效果
	 * @throws WebServerException if the server cannot be stopped
	 */
	void stop() throws WebServerException;

	/**
	 * Return the port this server is listening on.
	 * 返回当前服务器正在监听的端口，如果不存在，则返回-1
	 * @return the port (or -1 if none)
	 */
	int getPort();

	/**
	 * Initiates a graceful shutdown of the web server. Handling of new requests is
	 * prevented and the given {@code callback} is invoked at the end of the attempt. The
	 * attempt can be explicitly ended by invoking {@link #stop}. The default
	 * implementation invokes the callback immediately with
	 * {@link GracefulShutdownResult#IMMEDIATE}, i.e. no attempt is made at a graceful
	 * shutdown.
	 *
	 * 优雅的关闭web服务器，停止接收新的请求，并在最后尝试调用callback的回调方法；
	 * 也可以通过调用stop来停止，默认的实现时立马调用callback回调
	 *
	 * @param callback the callback to invoke when the graceful shutdown completes
	 * @since 2.3.0
	 */
	default void shutDownGracefully(GracefulShutdownCallback callback) {
		callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
	}

}
