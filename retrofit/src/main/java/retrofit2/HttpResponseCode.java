/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2;

/** HTTP response codes. */
public interface HttpResponseCode {
	
	// 2xx codes
	int OK = 200;
	int CREATED = 201;
	int ACCEPTED = 202;
	int UNAUTHORIZED = 203;
	int NO_CONTENT = 204;
	int RESET = 205;
	int PARTIAL_CONTENT = 206;
	// 3xx codes
	int PROXY_SWITCH = 306;
	// 4xx codes
	int INVALID_REQUEST = 400;
	int NOT_FOUND = 404;
}
