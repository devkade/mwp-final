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


class MachineEvent(models.Model):
    """운동기구 사용 이벤트"""
    EVENT_TYPES = [
        ('start', '사용 시작'),
        ('end', '사용 종료'),
    ]

    machine = models.ForeignKey(
        GymMachine,
        on_delete=models.CASCADE,
        related_name='events'
    )
    event_type = models.CharField(max_length=10, choices=EVENT_TYPES)
    image = models.ImageField(upload_to='events/%Y/%m/%d/')
    captured_at = models.DateTimeField(help_text="Edge에서 캡처한 시각")
    created_at = models.DateTimeField(auto_now_add=True)

    # YOLO 검출 결과
    person_count = models.IntegerField(default=0)
    detections = models.JSONField(
        default=dict,
        help_text="YOLO 검출 결과 JSON"
    )
    change_info = models.JSONField(
        default=dict,
        help_text="Change Detection 결과 JSON"
    )

    class Meta:
        ordering = ['-captured_at']
        indexes = [
            models.Index(fields=['machine', '-captured_at']),
            models.Index(fields=['event_type', '-captured_at']),
        ]

    def __str__(self):
        return f"{self.machine.name} - {self.get_event_type_display()} ({self.captured_at})"
