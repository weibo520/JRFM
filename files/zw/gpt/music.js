import axios from 'axios'
import axiosRetry from 'axios-retry'

const api = axios.create({
  baseURL: '/api',  // 修改为使用代理路径
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 配置请求重试
axiosRetry(api, { 
  retries: 3,
  retryDelay: (retryCount) => {
    return retryCount * 1000
  },
  retryCondition: (error) => {
    return axiosRetry.isNetworkOrIdempotentRequestError(error) || error.code === 'ECONNABORTED'
  }
})

export const musicApi = {
  // 获取推荐歌单
  getRecommendPlaylists() {
    return api.get('/recommend/playlist/u')
  },
  // 获取歌单详情
  getSonglistDetail(id) {
    return api.get('/songlist', { params: { id } })
  },
  // 获取轮播图
  getBanners() {
    return api.get('/recommend/banner')
  },
  // 搜索
  search(params) {
    return api.get('/search', { params })
  },
  // 获取歌曲播放地址
  getSongUrl(id) {
    return api.get('/song/url', { params: { id } })
  },
  // 获取歌手专辑
  getSingerAlbums(singermid, pageNo = 1, pageSize = 20) {
    return api.get('/singer/album', { 
      params: { singermid, pageNo, pageSize } 
    })
  },
  // 获取歌手歌曲
  getSingerDetail(singermid) {
    return api.get('/singer/songs', { params: { singermid } })
  },
  getSongDetail(songmid) {
    return api.get('/song/detail', { params: { songmid } })
  },
  
  // 获取歌词
  getLyric(songmid) {
    return api.get('/lyric', { params: { songmid } })
  }
}