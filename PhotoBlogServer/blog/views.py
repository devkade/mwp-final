from django.shortcuts import render, get_object_or_404, redirect
from django.utils import timezone
from .models import Post, ApiUser, GymMachine, MachineEvent
from .forms import PostForm
from rest_framework import viewsets, status
from rest_framework.decorators import action
from django.db.models import Count
from django.db.models.functions import TruncDate
from .serializers import (
    PostSerializer, GymMachineSerializer,
    MachineEventSerializer, MachineEventListSerializer, MachineEventCreateSerializer
)
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.pagination import PageNumberPagination
from rest_framework.status import HTTP_400_BAD_REQUEST, HTTP_401_UNAUTHORIZED, HTTP_200_OK
from rest_framework.authtoken.models import Token


def post_list(request):
    posts = Post.objects.filter(published_date__lte=timezone.now()).order_by('published_date')
    return render(request, 'blog/post_list.html', {'posts': posts})

def post_detail(request, pk):
    post = get_object_or_404(Post, pk=pk)
    return render(request, 'blog/post_detail.html', {'post': post})

def post_new(request):
    if request.method == "POST":
        form = PostForm(request.POST)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm()
    return render(request, 'blog/post_edit.html', {'form': form})

def post_edit(request, pk):
    post = get_object_or_404(Post, pk=pk)
    if request.method == "POST":
        form = PostForm(request.POST, instance=post)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm(instance=post)
    return render(request, 'blog/post_edit.html', {'form': form})

@api_view(['POST'])
@permission_classes([AllowAny])
def login(request):
    """
    Authenticate user with security_key and return token
    """
    security_key = request.data.get('security_key')

    if not security_key:
        return Response(
            {'error': 'security_key required'},
            status=HTTP_400_BAD_REQUEST
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
        }, status=HTTP_200_OK)
    except ApiUser.DoesNotExist:
        return Response(
            {'error': 'Invalid security key'},
            status=HTTP_401_UNAUTHORIZED
        )

class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer

    def perform_create(self, serializer):
        # 인증된 사용자를 author로 자동 설정하고 published_date도 설정
        serializer.save(author=self.request.user, published_date=timezone.now())


class GymMachineViewSet(viewsets.ModelViewSet):
    """운동기구 ViewSet"""
    queryset = GymMachine.objects.filter(is_active=True)
    serializer_class = GymMachineSerializer
    permission_classes = [IsAuthenticated]

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
    """이벤트 ViewSet for /api_root/events/"""
    queryset = MachineEvent.objects.all()
    permission_classes = [IsAuthenticated]
    ordering = ['-captured_at']

    def get_serializer_class(self):
        if self.action == 'list':
            return MachineEventListSerializer
        return MachineEventSerializer

    def get_queryset(self):
        queryset = super().get_queryset()

        # Filter by event_type
        event_type = self.request.query_params.get('event_type')
        if event_type:
            queryset = queryset.filter(event_type=event_type)

        # Filter by date range
        date_from = self.request.query_params.get('date_from')
        date_to = self.request.query_params.get('date_to')

        if date_from:
            queryset = queryset.filter(captured_at__date__gte=date_from)
        if date_to:
            queryset = queryset.filter(captured_at__date__lte=date_to)

        return queryset


@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def machine_events(request, machine_id):
    """
    GET: List events for a specific machine
    POST: Create a new event for a specific machine (Edge device)
    """
    # Verify machine exists
    machine = get_object_or_404(GymMachine, pk=machine_id, is_active=True)

    if request.method == 'GET':
        # List events for this machine
        queryset = MachineEvent.objects.filter(machine=machine)

        # Filter by event_type
        event_type = request.query_params.get('event_type')
        if event_type:
            queryset = queryset.filter(event_type=event_type)

        # Filter by date range
        date_from = request.query_params.get('date_from')
        date_to = request.query_params.get('date_to')
        if date_from:
            queryset = queryset.filter(captured_at__date__gte=date_from)
        if date_to:
            queryset = queryset.filter(captured_at__date__lte=date_to)

        # Apply pagination
        paginator = PageNumberPagination()
        page = paginator.paginate_queryset(queryset, request)
        if page is not None:
            serializer = MachineEventListSerializer(page, many=True)
            return paginator.get_paginated_response(serializer.data)

        serializer = MachineEventListSerializer(queryset, many=True)
        return Response(serializer.data)

    elif request.method == 'POST':
        # Create event for this machine
        serializer = MachineEventCreateSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(machine=machine)
            # Return full event data using MachineEventSerializer
            event = MachineEvent.objects.get(pk=serializer.instance.pk)
            response_serializer = MachineEventSerializer(event)
            return Response(response_serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)