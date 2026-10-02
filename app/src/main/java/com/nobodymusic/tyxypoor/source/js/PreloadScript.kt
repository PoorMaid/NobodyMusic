package com.nobodymusic.tyxypoor.source.js

object PreloadScript {
    const val JS = """
!function(){
'use strict';
var __nb = globalThis.__nb || (globalThis.__nb = {});
var events = {};
var requestHandler = null;
var inited = false;
var VERSION = '2.0.0';
function makeAes(){ throw new Error('unsupported'); }
var utils = {
  crypto: {
    aesEncrypt: function(buf, mode, key, iv){ return __nb.nativeAesEncrypt(buf, mode, key, iv); },
    rsaEncrypt: function(buf, key){ return __nb.nativeRsaEncrypt(buf, key); },
    randomBytes: function(n){ return __nb.nativeRandomBytes(n); },
    md5: function(str){ return __nb.nativeMd5(str); }
  },
  buffer: {
    from: function(a){ return __nb.nativeBufferFrom(a); },
    bufToString: function(b, f){ return __nb.nativeBufToString(b, f); }
  }
};
var lx = {
  version: VERSION,
  env: 'mobile',
  EVENT_NAMES: { request: 'request', inited: 'inited', updateAlert: 'updateAlert' },
  on: function(name, handler){
    events[name] = handler;
    return function(){ if(events[name] === handler) delete events[name]; };
  },
  send: function(name, data){
    if(name === 'inited'){ inited = true; __nb.onInited(JSON.stringify(data || null)); }
    else if(name === 'updateAlert'){ __nb.onUpdateAlert(JSON.stringify(data || null)); }
    return Promise.resolve();
  },
  request: function(url, options, cb){
    return __nb.request(url, JSON.stringify(options || {}), cb);
  },
  utils: utils
};
globalThis.lx = lx;
globalThis.lx_setup = function(key, id, name, version){
  __nb.setupInfo(JSON.stringify({ key: key, id: id, name: name, version: version }));
};
__nb.dispatch = function(source, action, infoJson){
  var handler = events['request'];
  if(!handler) return Promise.reject(new Error('no request handler'));
  var info = infoJson ? JSON.parse(infoJson) : {};
  try {
    return Promise.resolve(handler({ source: source, action: action, info: info }));
  } catch(e){ return Promise.reject(e); }
};
__nb.isInited = function(){ return inited; };
__nb.getAllSourceConfig = function(){
  var config = {
    kw: { name: '酷我音乐', type: 'music', actions: ['musicUrl'], qualitys: ['128k','320k','flac'] },
    kg: { name: '酷狗音乐', type: 'music', actions: ['musicUrl'], qualitys: ['128k','320k','flac'] },
    tx: { name: 'QQ音乐', type: 'music', actions: ['musicUrl'], qualitys: ['128k','320k','flac','flac24bit'] },
    wy: { name: '网易云音乐', type: 'music', actions: ['musicUrl'], qualitys: ['128k','320k','flac'] },
    mg: { name: '咪咕音乐', type: 'music', actions: ['musicUrl'], qualitys: ['128k','320k','flac'] }
  };
  return JSON.stringify(config);
};
})();
"""
}