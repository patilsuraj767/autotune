/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.common.data.experiments;

import java.net.URL;

/**
 *
 */
public class DatasourceInfo {
	private final String name;
	private final URL url;

	public DatasourceInfo(String name, URL url) {
		this.name = name;
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public URL getUrl() {
		return url;
	}
}