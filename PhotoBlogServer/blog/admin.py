from django.contrib import admin
from .models import Post, ApiUser, GymMachine

admin.site.register(Post)


@admin.register(ApiUser)
class ApiUserAdmin(admin.ModelAdmin):
    list_display = ['name', 'security_key_preview', 'user', 'is_active', 'created_at']
    search_fields = ['name', 'security_key', 'user__username']
    list_filter = ['is_active', 'created_at']

    def security_key_preview(self, obj):
        return f"{obj.security_key[:8]}..."
    security_key_preview.short_description = 'Security Key'


@admin.register(GymMachine)
class GymMachineAdmin(admin.ModelAdmin):
    list_display = ['name', 'machine_type', 'location', 'is_active', 'created_at']
    search_fields = ['name', 'location', 'description']
    list_filter = ['machine_type', 'location', 'is_active', 'created_at']
