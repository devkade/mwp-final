from django.contrib import admin
from django.utils.html import format_html
from .models import Post, ApiUser, GymMachine, MachineEvent

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


@admin.register(MachineEvent)
class MachineEventAdmin(admin.ModelAdmin):
    list_display = ['id', 'machine', 'event_type', 'person_count', 'captured_at', 'image_preview']
    list_filter = ['event_type', 'machine', 'captured_at']
    search_fields = ['machine__name', 'machine__location']
    date_hierarchy = 'captured_at'
    readonly_fields = ['created_at', 'image_preview_large']
    ordering = ['-captured_at']

    def image_preview(self, obj):
        if obj.image:
            return format_html('<img src="{}" width="50" height="50" style="object-fit: cover;" />', obj.image.url)
        return '-'
    image_preview.short_description = 'Image'

    def image_preview_large(self, obj):
        if obj.image:
            return format_html('<img src="{}" width="300" />', obj.image.url)
        return '-'
    image_preview_large.short_description = 'Image Preview'
