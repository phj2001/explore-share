import request from '@/utils/request.js'

export const createRoute = (data) => {
  return request.post('/user-routes', data)
}

export const updateRoute = (id, data) => {
  return request.put(`/user-routes/${id}`, data)
}

export const deleteRoute = (id) => {
  return request.delete(`/user-routes/${id}`)
}

export const getPublicRoutes = (params = {}) => {
  return request.get('/user-routes', { params })
}

export const getRouteDetail = (id) => {
  return request.get(`/user-routes/${id}`)
}

export const toggleRouteLike = (id) => {
  return request.post(`/user-routes/${id}/like`)
}

export const toggleRouteFavorite = (id) => {
  return request.post(`/user-routes/${id}/favorite`)
}

export const getMyRoutes = (params = {}) => {
  return request.get('/users/me/routes', { params })
}

export const getMyFavoriteRoutes = (params = {}) => {
  return request.get('/users/me/favorite-routes', { params })
}
