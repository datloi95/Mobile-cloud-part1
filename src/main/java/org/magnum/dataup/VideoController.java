/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.multipart.MultipartFile;
import retrofit.*;
import retrofit.http.Multipart;

import static javax.servlet.http.HttpServletResponse.SC_OK;

@Controller
public class VideoController {


	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "VideoController"
	 * 
	 * 
		 ________  ________  ________  ________          ___       ___  ___  ________  ___  __       
		|\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \     
		\ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_   
		 \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \  
		  \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \ 
		   \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
		    \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
                                                                                                                                                                                                                                                                        
	 * 
	 */
	private static final AtomicLong currentId = new AtomicLong(1L);
	private Map<Long,Video> videos = new HashMap<>();
	private VideoFileManager videoDataMgr;


	private String getDataUrl(long videoId){
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String base =
				"http://"+request.getServerName()
						+ ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
		return base;
	}

	public Video save(Video entity) {
		checkAndSetId(entity);
		videos.put(entity.getId(), entity);
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}

	public void saveSomeVideo(Video v, MultipartFile videoData) throws IOException {
		videoDataMgr.saveVideoData(v, videoData.getInputStream());
	}

	public void serveSomeVideo(Video v, HttpServletResponse response) throws IOException {
		// Of course, you would need to send some headers, etc. to the
		// client too!
		//  ...
		videoDataMgr.copyVideoData(v, response.getOutputStream());
	}

	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return videos.values();
	}

	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		save(v);
		v.setDataUrl(getDataUrl(v.getId()));
		return v;
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus uploadVideo (@PathVariable("id") long id, @RequestParam("data") MultipartFile videoData, HttpServletResponse response) throws IOException {

		VideoStatus status = new VideoStatus(VideoStatus.VideoState.PROCESSING);

		Video v = videos.get(id);
		if (v == null){
			response.setStatus(404);
			return status;
		}
		try {
			videoDataMgr = VideoFileManager.get();
			this.saveSomeVideo(v, videoData);

		} catch (IOException e) {
			e.printStackTrace();
		}

		status.setState(VideoStatus.VideoState.READY);
		return status;
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public @ResponseBody void downloadVideo(@PathVariable("id") long id, HttpServletResponse response) throws IOException {

		Video v = videos.get(id);
		if (v == null){
			response.setStatus(404);
			return;
		}

		try {
			videoDataMgr = VideoFileManager.get();
			this.serveSomeVideo(v, response);

		} catch (IOException e) {
			e.printStackTrace();
		}
		if (response.getOutputStream() == null){
			response.setStatus(404);
			return;
		}

	}



}
