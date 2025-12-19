from django.db import router
from django.urls import path, include
from . import views
from rest_framework import routers

router = routers.DefaultRouter()
router.register('Post', views.BlogImages)

urlpatterns = [
    path("", views.gym_login, name="gym_login"),
    path("dashboard/", views.dashboard, name="dashboard"),
    path("login/", views.gym_login, name="login_alias"), # Optional alias if needed, or remove
    path("equipment/<int:pk>/", views.equipment_detail, name="equipment_detail"),
    path("equipment/<int:pk>/history/", views.usage_history, name="usage_history"),
    path("event/<int:pk>/", views.event_detail, name="event_detail"),

    path("machines/", views.dashboard, name="machine_list"),
    path("users/", views.user_list, name="user_list"),
    path("logout/", views.web_logout, name="web_logout"),
    path('post/', views.post_list, name="post_list"),
    path('post/<int:pk>/', views.post_detail, name="post_detail"),
    path('post/new/', views.post_new, name="post_new"),
    path('post/<int:pk>/edit/', views.post_edit, name="post_edit"),
    path('api_root/',include(router.urls)),

