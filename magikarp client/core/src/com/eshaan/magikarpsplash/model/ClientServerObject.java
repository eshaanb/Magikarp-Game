package com.eshaan.magikarpsplash.model;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by ebhalla on 10/9/14.
 */
public class ClientServerObject {

	public List<String> ipList;
	public String action;
	public String from;
	public String iptochallenge;
	public float x;
	public float y;
	public float yvel;


	public ClientServerObject() {

	}

	public byte[] toJson() {
		Json json = new Json();
		json.setOutputType(JsonWriter.OutputType.json);
		return json.toJson(this).getBytes(Charset.forName("UTF-8"));
	}
}
