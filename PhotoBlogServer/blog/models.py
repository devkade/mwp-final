from django.conf import settings
from django.db import models
from django.utils import timezone


class Post(models.Model):
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    title = models.CharField(max_length=200)
    text = models.TextField()
    created_date = models.DateTimeField(default=timezone.now)
    published_date = models.DateTimeField(blank=True, null=True)
    image = models.ImageField(upload_to='blog_image/%Y/%m/%d/', default='blog_image/default_error.png')

    def publish(self):
        self.published_date = timezone.now()
        self.save()

    def __str__(self):
        return self.title


class ApiUser(models.Model):
    """보안키 기반 API 사용자"""
    name = models.CharField(max_length=100)
    security_key = models.CharField(max_length=64, unique=True)
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='api_user'
    )
    created_at = models.DateTimeField(auto_now_add=True)
    is_active = models.BooleanField(default=True)

    def __str__(self):
        return f"{self.name} ({self.security_key[:8]}...)"


class GymMachine(models.Model):
    """운동기구 정보"""
    MACHINE_TYPES = [
        ('treadmill', '런닝머신'),
        ('bench_press', '벤치프레스'),
        ('squat_rack', '스쿼트랙'),
        ('lat_pulldown', '랫풀다운'),
        ('leg_press', '레그프레스'),
        ('cable_machine', '케이블머신'),
        ('dumbbell', '덤벨존'),
        ('other', '기타'),
    ]

    name = models.CharField(max_length=100)
    machine_type = models.CharField(max_length=20, choices=MACHINE_TYPES)
    location = models.CharField(max_length=100, help_text="예: 1층 A구역")
    description = models.TextField(blank=True)
    thumbnail = models.ImageField(
        upload_to='machines/',
        blank=True,
        null=True
    )
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['location', 'name']

    def __str__(self):
        return f"{self.name} ({self.location})"
