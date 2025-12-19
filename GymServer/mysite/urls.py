"""
URL configuration for mysite project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/5.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""

from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from rest_framework.authtoken.views import obtain_auth_token
from blog import views
from rest_framework import routers
router = routers.DefaultRouter()
router.register('Post', views.BlogImages)
router.register('machines', views.GymMachineViewSet)
router.register('events', views.MachineEventViewSet)

urlpatterns = [
    path('', views.gym_login, name='gym_login_root'),
    path('post/<int:pk>/', views.post_detail, name='post_detail'),
    path('post/new/', views.post_new, name='post_new'),
    path('post/<int:pk>/edit/', views.post_edit, name='post_edit'),
    path('api/auth/login/', views.login, name='api-login'),
    path('api/machines/<int:machine_id>/events/', views.machine_events, name='machine-events'),
    path('api_root/', include(router.urls)),
    path('admin/', admin.site.urls),
    path('api-token-auth/', obtain_auth_token),
    path('gym/login/', views.gym_login, name='gym_login'),
    path('dashboard/', views.dashboard, name='dashboard'),
    path('machines/', views.dashboard, name='machine_list'),
    path('equipment/<int:pk>/', views.equipment_detail, name='equipment_detail'),
    path('equipment/<int:pk>/history/', views.usage_history, name='usage_history'),
    path('event/<int:pk>/', views.event_detail, name='event_detail'),

    path('users/', views.user_list, name='user_list'),
    path('users/add/', views.user_add, name='user_add'),
    path('users/<int:pk>/delete/', views.user_delete, name='user_delete'),
    path('machines/add/', views.machine_add, name='machine_add'),
    path('logout/', views.web_logout, name='web_logout'),
]

urlpatterns += static(settings.STATIC_URL, document_root=settings.STATIC_ROOT)
urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
