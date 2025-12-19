from blog.models import Post, GymMachine, MachineEvent
from rest_framework import serializers
from django.contrib.auth.models import User
import json


class PostSerializer(serializers.HyperlinkedModelSerializer):
    author = serializers.PrimaryKeyRelatedField(read_only=True)

    class Meta:
        model = Post
        fields = ('id', 'author', 'title', 'text','created_date','published_date', 'image')


class SecurityKeyLoginSerializer(serializers.Serializer):
    security_key = serializers.CharField(max_length=64)


class MachineEventSerializer(serializers.ModelSerializer):
    """Full event serializer for detail view"""
    machine_name = serializers.CharField(source='machine.name', read_only=True)
    event_type_display = serializers.CharField(
        source='get_event_type_display',
        read_only=True
    )

    class Meta:
        model = MachineEvent
        fields = [
            'id', 'machine', 'machine_name',
            'event_type', 'event_type_display',
            'image', 'captured_at', 'created_at',
            'person_count', 'detections', 'change_info'
        ]
        read_only_fields = ['created_at']


class MachineEventListSerializer(serializers.ModelSerializer):
    """Lightweight serializer for list view"""
    machine_name = serializers.CharField(source='machine.name', read_only=True)
    event_type_display = serializers.CharField(
        source='get_event_type_display',
        read_only=True
    )

    class Meta:
        model = MachineEvent
        fields = [
            'id', 'machine', 'machine_name',
            'event_type', 'event_type_display',
            'image', 'captured_at', 'person_count'
        ]


class MachineEventCreateSerializer(serializers.ModelSerializer):
    """Serializer for creating events (POST from Edge)"""
    detections = serializers.JSONField(required=False, default=dict)
    change_info = serializers.JSONField(required=False, default=dict)

    class Meta:
        model = MachineEvent
        fields = [
            'event_type', 'image', 'captured_at',
            'person_count', 'detections', 'change_info'
        ]

    def validate_detections(self, value):
        """Handle string JSON input from multipart form"""
        if isinstance(value, str):
            try:
                return json.loads(value)
            except json.JSONDecodeError:
                raise serializers.ValidationError("Invalid JSON format for detections")
        return value

    def validate_change_info(self, value):
        """Handle string JSON input from multipart form"""
        if isinstance(value, str):
            try:
                return json.loads(value)
            except json.JSONDecodeError:
                raise serializers.ValidationError("Invalid JSON format for change_info")
        return value


class GymMachineSerializer(serializers.ModelSerializer):
    event_count = serializers.SerializerMethodField()
    last_event = serializers.SerializerMethodField()

    class Meta:
        model = GymMachine
        fields = [
            'id', 'name', 'machine_type', 'location',
            'description', 'thumbnail', 'is_active',
            'event_count', 'last_event'
        ]

    def get_event_count(self, obj):
        # MachineEvent model will be added in Epic 2
        # For now, check if events relation exists
        if hasattr(obj, 'events'):
            return obj.events.count()
        return 0

    def get_last_event(self, obj):
        # MachineEvent model will be added in Epic 2
        # For now, check if events relation exists
        if hasattr(obj, 'events'):
            last = obj.events.first()
            if last:
                return {
                    'event_type': last.event_type,
                    'captured_at': last.captured_at.isoformat()
                }
        return None