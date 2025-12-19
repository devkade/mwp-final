from blog.models import Post, GymMachine
from rest_framework import serializers
from django.contrib.auth.models import User


class PostSerializer(serializers.HyperlinkedModelSerializer):
    author = serializers.PrimaryKeyRelatedField(read_only=True)

    class Meta:
        model = Post
        fields = ('id', 'author', 'title', 'text','created_date','published_date', 'image')


class SecurityKeyLoginSerializer(serializers.Serializer):
    security_key = serializers.CharField(max_length=64)


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