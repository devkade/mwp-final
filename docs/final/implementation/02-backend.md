# 백엔드 구현 스펙 (PhotoBlogServer)

## 신규 모델 정의

### ApiUser 모델

```python
# blog/models.py

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
```

### GymMachine 모델

```python
# blog/models.py

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
```

### MachineEvent 모델

```python
# blog/models.py

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
```

## Serializers

```python
# blog/serializers.py

from .models import GymMachine, MachineEvent, ApiUser

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
        return obj.events.count()

    def get_last_event(self, obj):
        last = obj.events.first()
        if last:
            return {
                'event_type': last.event_type,
                'captured_at': last.captured_at.isoformat()
            }
        return None


class MachineEventSerializer(serializers.ModelSerializer):
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
    """목록 조회용 경량 시리얼라이저"""
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


class SecurityKeyLoginSerializer(serializers.Serializer):
    security_key = serializers.CharField(max_length=64)
```

## Views

```python
# blog/views.py

from rest_framework import viewsets, status, filters
from rest_framework.decorators import action
from rest_framework.response import Response
from django_filters.rest_framework import DjangoFilterBackend
from django.db.models import Count
from django.db.models.functions import TruncDate
from .models import GymMachine, MachineEvent, ApiUser


class GymMachineViewSet(viewsets.ModelViewSet):
    """운동기구 ViewSet"""
    queryset = GymMachine.objects.filter(is_active=True)
    serializer_class = GymMachineSerializer

    @action(detail=True, methods=['get'])
    def stats(self, request, pk=None):
        """기구별 통계"""
        machine = self.get_object()
        date_from = request.query_params.get('date_from')
        date_to = request.query_params.get('date_to')

        events = machine.events.all()
        if date_from:
            events = events.filter(captured_at__date__gte=date_from)
        if date_to:
            events = events.filter(captured_at__date__lte=date_to)

        # 일별 사용 횟수
        daily_stats = events.filter(event_type='start').annotate(
            date=TruncDate('captured_at')
        ).values('date').annotate(
            count=Count('id')
        ).order_by('date')

        return Response({
            'machine_id': machine.id,
            'machine_name': machine.name,
            'total_starts': events.filter(event_type='start').count(),
            'total_ends': events.filter(event_type='end').count(),
            'daily_usage': list(daily_stats)
        })


class MachineEventViewSet(viewsets.ModelViewSet):
    """이벤트 ViewSet"""
    queryset = MachineEvent.objects.all()
    filter_backends = [DjangoFilterBackend, filters.OrderingFilter]
    filterset_fields = ['machine', 'event_type']
    ordering_fields = ['captured_at', 'created_at']
    ordering = ['-captured_at']

    def get_serializer_class(self):
        if self.action == 'list':
            return MachineEventListSerializer
        return MachineEventSerializer

    def get_queryset(self):
        queryset = super().get_queryset()

        # 날짜 필터
        date_from = self.request.query_params.get('date_from')
        date_to = self.request.query_params.get('date_to')

        if date_from:
            queryset = queryset.filter(captured_at__date__gte=date_from)
        if date_to:
            queryset = queryset.filter(captured_at__date__lte=date_to)

        return queryset


@api_view(['POST'])
@permission_classes([AllowAny])
def security_key_login(request):
    """보안키 로그인"""
    security_key = request.data.get('security_key')

    if not security_key:
        return Response(
            {'error': 'security_key required'},
            status=status.HTTP_400_BAD_REQUEST
        )

    try:
        api_user = ApiUser.objects.get(
            security_key=security_key,
            is_active=True
        )
        token, _ = Token.objects.get_or_create(user=api_user.user)
        return Response({
            'token': token.key,
            'name': api_user.name
        })
    except ApiUser.DoesNotExist:
        return Response(
            {'error': 'Invalid security key'},
            status=status.HTTP_401_UNAUTHORIZED
        )
```

## URL 설정

```python
# mysite/urls.py (추가)

from rest_framework.routers import DefaultRouter

router = DefaultRouter()
router.register('Post', views.BlogImages)  # 기존 유지 (호환성)
router.register('machines', views.GymMachineViewSet)
router.register('events', views.MachineEventViewSet)

urlpatterns = [
    # 기존 URL 유지
    path('api_root/', include(router.urls)),

    # 신규 API
    path('api/auth/login/', views.security_key_login, name='security-key-login'),
    path('api/machines/<int:machine_id>/events/',
         views.MachineEventViewSet.as_view({'get': 'list', 'post': 'create'}),
         name='machine-events'),
    # ...
]
```

## 데이터베이스 마이그레이션

```bash
# 실행 순서
cd PhotoBlogServer
python manage.py makemigrations blog
python manage.py migrate
python manage.py createsuperuser  # ApiUser 생성용
```
