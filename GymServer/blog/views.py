from django.shortcuts import render, get_object_or_404, redirect
from django.utils import timezone
from django.db.models import Count, Max
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
from django.contrib.auth import login as django_login, logout as django_logout
from django.contrib.auth.models import User



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

        # Filter by machine
        machine_id = self.request.query_params.get('machine')
        if machine_id:
            queryset = queryset.filter(machine_id=machine_id)

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
@permission_classes([IsAuthenticated])
def usage_history(request, pk):
    if not request.user.is_authenticated:
        return redirect('gym_login')
        
    machine = get_object_or_404(GymMachine, pk=pk)
    events = machine.events.all().order_by('-captured_at')

    # Filter by event_type
    event_type = request.GET.get('event_type')
    if event_type in ['start', 'end']:
        events = events.filter(event_type=event_type)
    
    context = {
        'machine': machine,
        'events': events,
        'current_filter': event_type
    }
    return render(request, 'blog/usage_history.html', context)

@permission_classes([IsAuthenticated])
def event_detail(request, pk):
    if not request.user.is_authenticated:
        return redirect('gym_login')
        
    event = get_object_or_404(MachineEvent, pk=pk)
    return render(request, 'blog/event_detail.html', {'event': event})



def gym_login(request):
    if request.method == 'POST':
        key = request.POST.get('security_key')
        try:
            api_user = ApiUser.objects.get(security_key=key, is_active=True)
            django_login(request, api_user.user)
            return redirect('dashboard')
        except ApiUser.DoesNotExist:
            return render(request, 'blog/gym_login.html', {'error': 'Invalid security key'})
            
    if request.user.is_authenticated:
        return redirect('dashboard')
    return render(request, 'blog/gym_login.html')

@permission_classes([IsAuthenticated])
def dashboard(request):
    if not request.user.is_authenticated:
        return redirect('gym_login')
        
    # Annotate with event count and last event time
    machines = GymMachine.objects.all().annotate(
        event_count=Count('events'),
        last_event_time=Max('events__captured_at')
    )
    total_assets = machines.count()
    
    context = {
        'machines': machines,
        'total_assets': total_assets,
    }
    return render(request, 'blog/dashboard.html', context)

@permission_classes([IsAuthenticated])
def user_list(request):
    if not request.user.is_authenticated:
        return redirect('gym_login')
    
    api_users = ApiUser.objects.all().order_by('-created_at')
    
    context = {
        'api_users': api_users
    }
    return render(request, 'blog/user_list.html', context)


@permission_classes([IsAuthenticated])
def equipment_detail(request, pk):
    if not request.user.is_authenticated:
        return redirect('gym_login')
        
    machine = get_object_or_404(GymMachine, pk=pk)
    return render(request, 'blog/equipment_detail.html', {'machine': machine})


@permission_classes([IsAuthenticated])
def user_add(request):
    if not request.user.is_authenticated:
        return redirect('gym_login')
        
    if request.method == 'POST':
        name = request.POST.get('name')
        security_key = request.POST.get('security_key')
        
        if name and security_key:
            # Create a unique username
            base_username = name.lower().replace(" ", "")
            username = base_username
            counter = 1
            while User.objects.filter(username=username).exists():
                username = f"{base_username}{counter}"
                counter += 1
            
            user = User.objects.create_user(username=username, password=security_key)
            
            ApiUser.objects.create(
                user=user,
                name=name,
                security_key=security_key,
                is_active=True
            )
            return redirect('user_list')
            
    return render(request, 'blog/user_form.html')

@permission_classes([IsAuthenticated])
def user_delete(request, pk):
    if not request.user.is_authenticated:
        return redirect('gym_login')
        
    if request.method == 'POST':
        api_user = get_object_or_404(ApiUser, pk=pk)
        user = api_user.user
        api_user.delete()
        if user:
            user.delete()
            
    return redirect('user_list')

@permission_classes([IsAuthenticated])
def machine_add(request):
    if not request.user.is_authenticated:
        return redirect('gym_login')
        
    if request.method == 'POST':
        name = request.POST.get('name')
        machine_type = request.POST.get('machine_type')
        location = request.POST.get('location')
        description = request.POST.get('description')
        thumbnail = request.FILES.get('thumbnail')
        
        if name and machine_type:
            GymMachine.objects.create(
                name=name,
                machine_type=machine_type,
                location=location,
                description=description,
                thumbnail=thumbnail,
                is_active=True
            )
            return redirect('machine_list')
            
    return render(request, 'blog/machine_form.html')

def web_logout(request):
    django_logout(request)
    return redirect('gym_login')
