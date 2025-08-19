// 等待DOM加载完成
document.addEventListener('DOMContentLoaded', function() {
    
    // 导航栏平滑滚动功能
    const navLinks = document.querySelectorAll('.nav-link');
    
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // 移除所有活动状态
            navLinks.forEach(nav => nav.classList.remove('active'));
            // 添加当前链接的活动状态
            this.classList.add('active');
            
            // 获取目标元素
            const targetId = this.getAttribute('href');
            const targetElement = document.querySelector(targetId);
            
            if (targetElement) {
                // 计算滚动位置（考虑固定导航栏高度）
                const navHeight = document.querySelector('.navbar').offsetHeight;
                const targetPosition = targetElement.offsetTop - navHeight;
                
                // 平滑滚动
                window.scrollTo({
                    top: targetPosition,
                    behavior: 'smooth'
                });
            }
        });
    });
    
    // 滚动时更新导航栏活动状态
    window.addEventListener('scroll', function() {
        const sections = document.querySelectorAll('section');
        const navHeight = document.querySelector('.navbar').offsetHeight;
        const scrollPosition = window.scrollY + navHeight + 100;
        
        sections.forEach(section => {
            const sectionTop = section.offsetTop;
            const sectionHeight = section.offsetHeight;
            const sectionId = section.getAttribute('id');
            
            if (scrollPosition >= sectionTop && scrollPosition < sectionTop + sectionHeight) {
                // 移除所有活动状态
                navLinks.forEach(nav => nav.classList.remove('active'));
                // 添加对应链接的活动状态
                const activeLink = document.querySelector(`.nav-link[href="#${sectionId}"]`);
                if (activeLink) {
                    activeLink.classList.add('active');
                }
            }
        });
        
        // 导航栏滚动效果
        const navbar = document.querySelector('.navbar');
        if (window.scrollY > 50) {
            navbar.style.background = 'rgba(255, 255, 255, 0.95)';
            navbar.style.backdropFilter = 'blur(10px)';
        } else {
            navbar.style.background = 'var(--white)';
            navbar.style.backdropFilter = 'none';
        }
    });
    
    // 联系表单处理
    const contactForm = document.querySelector('.contact-form');
    if (contactForm) {
        contactForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // 获取表单数据
            const formData = new FormData(this);
            const name = this.querySelector('input[type="text"]').value;
            const email = this.querySelector('input[type="email"]').value;
            const message = this.querySelector('textarea').value;
            
            // 简单验证
            if (!name || !email || !message) {
                showMessage('请填写所有必填字段', 'error');
                return;
            }
            
            // 邮箱格式验证
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                showMessage('请输入有效的邮箱地址', 'error');
                return;
            }
            
            // 模拟表单提交
            const submitBtn = this.querySelector('.btn-submit');
            const originalText = submitBtn.textContent;
            submitBtn.textContent = '发送中...';
            submitBtn.disabled = true;
            
            // 模拟异步提交
            setTimeout(() => {
                showMessage('消息发送成功！我们会尽快回复您。', 'success');
                this.reset();
                submitBtn.textContent = originalText;
                submitBtn.disabled = false;
            }, 1500);
        });
    }
    
    // 显示消息提示
    function showMessage(text, type) {
        // 创建消息元素
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.textContent = text;
        
        // 添加样式
        messageDiv.style.cssText = `
            position: fixed;
            top: 90px;
            right: 20px;
            padding: 15px 20px;
            border-radius: 8px;
            color: white;
            font-weight: 500;
            z-index: 10000;
            transform: translateX(400px);
            transition: transform 0.3s ease;
            ${type === 'success' 
                ? 'background: linear-gradient(135deg, #10b981, #059669);' 
                : 'background: linear-gradient(135deg, #ef4444, #dc2626);'
            }
        `;
        
        document.body.appendChild(messageDiv);
        
        // 显示动画
        setTimeout(() => {
            messageDiv.style.transform = 'translateX(0)';
        }, 100);
        
        // 自动隐藏
        setTimeout(() => {
            messageDiv.style.transform = 'translateX(400px)';
            setTimeout(() => {
                document.body.removeChild(messageDiv);
            }, 300);
        }, 3000);
    }
    
    // 按钮点击效果
    const buttons = document.querySelectorAll('button');
    buttons.forEach(button => {
        button.addEventListener('click', function(e) {
            // 创建涟漪效果
            const ripple = document.createElement('span');
            const rect = this.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            const x = e.clientX - rect.left - size / 2;
            const y = e.clientY - rect.top - size / 2;
            
            ripple.style.cssText = `
                position: absolute;
                width: ${size}px;
                height: ${size}px;
                left: ${x}px;
                top: ${y}px;
                background: rgba(255, 255, 255, 0.3);
                border-radius: 50%;
                transform: scale(0);
                animation: ripple 0.6s linear;
                pointer-events: none;
            `;
            
            this.style.position = 'relative';
            this.style.overflow = 'hidden';
            this.appendChild(ripple);
            
            setTimeout(() => {
                if (ripple.parentNode) {
                    ripple.parentNode.removeChild(ripple);
                }
            }, 600);
        });
    });
    
    // 添加涟漪动画CSS
    const style = document.createElement('style');
    style.textContent = `
        @keyframes ripple {
            to {
                transform: scale(4);
                opacity: 0;
            }
        }
        
        /* 文章卡片悬停效果增强 */
        .article-card {
            position: relative;
            overflow: hidden;
        }
        
        .article-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.05), transparent);
            transition: left 0.5s ease;
        }
        
        .article-card:hover::before {
            left: 100%;
        }
        
        /* 统计数字动画 */
        .stat h3 {
            opacity: 0;
            transform: translateY(20px);
            animation: countUp 1s ease forwards;
        }
        
        @keyframes countUp {
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
    `;
    document.head.appendChild(style);
    
    // Intersection Observer for animations
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
                
                // 如果是统计数字，启动计数动画
                if (entry.target.classList.contains('stat')) {
                    animateCounter(entry.target.querySelector('h3'));
                }
            }
        });
    }, observerOptions);
    
    // 观察需要动画的元素
    const animatedElements = document.querySelectorAll('.article-card, .stat, .about-text, .contact-info, .contact-form');
    animatedElements.forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(30px)';
        el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
        observer.observe(el);
    });
    
    // 数字计数动画
    function animateCounter(element) {
        const target = parseInt(element.textContent.replace(/[^\d]/g, ''));
        const suffix = element.textContent.replace(/[\d]/g, '');
        let current = 0;
        const increment = target / 50;
        const timer = setInterval(() => {
            current += increment;
            if (current >= target) {
                element.textContent = target + suffix;
                clearInterval(timer);
            } else {
                element.textContent = Math.floor(current) + suffix;
            }
        }, 30);
    }
    
    // Logo点击回到顶部
    const logo = document.querySelector('.logo a');
    if (logo) {
        logo.addEventListener('click', function(e) {
            if (this.getAttribute('href') === '#home') {
                e.preventDefault();
                window.scrollTo({
                    top: 0,
                    behavior: 'smooth'
                });
                
                // 更新导航状态
                navLinks.forEach(nav => nav.classList.remove('active'));
                document.querySelector('.nav-link[href="#home"]').classList.add('active');
            }
        });
    }
    
    
    // 登录注册弹窗功能
    initAuthModals();
    
    // 初始化3D地球交互
    /* Spinning globe removed */
    
    // 初始化文章功能
    // Load articles manifest
    fetch('assets/Articles/articles.json')
        .then(r=>r.json())
        .then(articlesArr=>{
            window.__articlesManifest = articlesArr;
            initArticleFeatures(articlesArr);
        })
        .catch(err=>{
            console.error('Failed to load articles manifest', err);
        });
    
    // 初始化帖子滑动功能
    initPostSlider();
    
    // 初始化静态世界地图
    initWorldMap();
    
});

// 文章功能初始化
function initArticleFeatures(articlesArr) {
    const menuContainer=document.querySelector('.articles-menu');
    const displayContainer=document.querySelector('.article-display');

    // clear containers
    menuContainer.innerHTML='';
    displayContainer.innerHTML='';

    articlesArr.forEach((art,index)=>{
        // menu button
        const btn=document.createElement('button');
        btn.className=`article-menu-btn${index===0?' active':''}`;
        btn.dataset.article=art.id;
        btn.textContent=art.title;
        menuContainer.appendChild(btn);

        // preview card
        const item=document.createElement('div');
        item.className=`article-item${index===0?' active':''}`;
        item.dataset.article=art.id;
        item.innerHTML=`<div class="article-cover"><div class="article-date">${art.date}</div><div class="article-image-placeholder">${art.cover||'Cover'}</div></div><div class="article-content"><h3 class="article-title">${art.title}</h3><p class="article-intro">${art.intro||''}</p><button class="btn-read-article" data-article="${art.id}">Read this article</button></div>`;
        displayContainer.appendChild(item);
    });

    const menuButtons=menuContainer.querySelectorAll('.article-menu-btn');
    const articleItems=displayContainer.querySelectorAll('.article-item');
    const readArticleButtons=document.querySelectorAll('.btn-read-article, .btn-team-article');
    const articleModal = document.getElementById('articleModal');
    
    console.log('Article features initialized:', {
        menuButtons: menuButtons.length,
        articleItems: articleItems.length,
        readArticleButtons: readArticleButtons.length
    });
    
    // 缓存已加载的文章数据
    const articleCache = {};

    // (Removed unused realArticles sample configuration block)

    function loadArticleData(articleId){
        if(articleCache[articleId]) return Promise.resolve(articleCache[articleId]);
        const info = (window.__articlesManifest||[]).find(a=>a.id===articleId);
        if(!info) return Promise.reject('Article not configured');

        const filePath = `assets/Articles/${info.file}`;
        return fetch(filePath)
            .then(r=>r.arrayBuffer())
            .then(buffer=>mammoth.convertToHtml({arrayBuffer:buffer}))
            .then(result=>{
                // Try to pull first image src (base64) from HTML
                let imgSrc='';
                const tmp=document.createElement('div');
                tmp.innerHTML=result.value||'';
                const imgEl=tmp.querySelector('img');
                if(imgEl){
                    imgSrc=imgEl.getAttribute('src')||'';
                }
                if(!imgSrc && info.cover){
                    imgSrc=`assets/Articles/${info.cover}`;
                }

                const data={
                    title:info.title,
                    date:info.date,
                    image:imgSrc,
                    content:result.value
                };
                articleCache[articleId]=data;
                return data;
            });
    }
    
    // 文章菜单切换功能
    menuButtons.forEach(button => {
        button.addEventListener('click', function() {
            const articleId = this.getAttribute('data-article');
            console.log('Article button clicked:', articleId);
            
            // 更新按钮状态
            menuButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');
            
            // 切换文章显示
            articleItems.forEach(item => {
                item.classList.remove('active');
                if (item.getAttribute('data-article') === articleId) {
                    console.log('Activating article item:', articleId);
                    setTimeout(() => {
                        item.classList.add('active');
                    }, 100);
                }
            });
        });
    });
    
    // 文章详情弹窗功能
    readArticleButtons.forEach(button => {
        button.addEventListener('click', function() {
            const articleId = this.getAttribute('data-article');

            loadArticleData(articleId)
                .then(articleData => {
                    document.getElementById('articleModalTitle').textContent = articleData.title;
                    document.getElementById('articleModalDate').textContent = articleData.date;
                    const imgHolder = document.getElementById('articleModalImage');
                    if(imgHolder){
                        if(articleData.image){
                            imgHolder.innerHTML=`<img src="assets/Articles/${articleData.image}" alt="${articleData.title}" style="width:100%;height:auto;border-radius:10px;">`;
                        } else {
                            imgHolder.innerHTML='';
                        }
                    }
                    document.getElementById('articleModalContent').innerHTML = articleData.content;

                    showModal('articleModal');
                })
                .catch(err => console.error('Failed to load article', err));
        });
    });
}

// 帖子滑动功能
function initPostSlider() {
    const slider = document.querySelector('.posts-slider');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const readMoreButtons = document.querySelectorAll('.btn-read-more-post');
    
    if (!slider || !prevBtn || !nextBtn) return;
    
    const cardWidth = 325; // 卡片宽度 + 间距
    
    // 帖子数据
    const postsData = {
        '1': {
            title: 'Future of Machine Learning',
            date: 'Dec 18, 2024',
            image: 'AI Research Post',
            content: `
                <p>Machine Learning continues to evolve at an unprecedented pace, transforming industries and reshaping our understanding of artificial intelligence capabilities.</p>
                
                <h3>Latest Developments</h3>
                <p>Recent breakthroughs in deep learning, neural networks, and reinforcement learning are opening new possibilities for solving complex real-world problems.</p>
                
                <h3>Key Areas of Growth</h3>
                <ul>
                    <li>Natural Language Processing and Understanding</li>
                    <li>Computer Vision and Image Recognition</li>
                    <li>Autonomous Systems and Robotics</li>
                    <li>Predictive Analytics and Decision Making</li>
                </ul>
                
                <h3>Industry Applications</h3>
                <p>From healthcare diagnostics to financial trading, machine learning algorithms are becoming increasingly sophisticated and reliable, enabling automation of complex decision-making processes.</p>
                
                <p>The future promises even more exciting developments as we continue to push the boundaries of what's possible with artificial intelligence.</p>
            `
        },
        '2': {
            title: 'Blockchain Revolution',
            date: 'Dec 16, 2024',
            image: 'Tech Innovation Post',
            content: `
                <p>Blockchain technology is expanding far beyond its cryptocurrency origins, revolutionizing how we think about trust, transparency, and decentralization in digital systems.</p>
                
                <h3>Beyond Cryptocurrency</h3>
                <p>While Bitcoin introduced blockchain to the world, the technology's applications now span supply chain management, digital identity, smart contracts, and decentralized finance.</p>
                
                <h3>Key Benefits</h3>
                <ul>
                    <li>Immutable record keeping and transparency</li>
                    <li>Elimination of intermediaries and reduced costs</li>
                    <li>Enhanced security through cryptographic protection</li>
                    <li>Global accessibility and borderless transactions</li>
                </ul>
                
                <h3>Emerging Use Cases</h3>
                <p>Industries are discovering innovative applications, from healthcare record management to voting systems, real estate transactions, and intellectual property protection.</p>
                
                <p>As the technology matures, we're seeing the emergence of more sustainable and scalable blockchain solutions that address earlier limitations.</p>
            `
        },
        '3': {
            title: 'Design Thinking in Tech',
            date: 'Dec 14, 2024',
            image: 'Design Thinking Post',
            content: `
                <p>Design thinking methodology is transforming how technology companies approach product development, putting human needs at the center of innovation processes.</p>
                
                <h3>The Human-Centered Approach</h3>
                <p>By emphasizing empathy, ideation, and experimentation, design thinking helps teams create solutions that truly resonate with users and solve real problems.</p>
                
                <h3>Core Principles</h3>
                <ul>
                    <li>Empathize with users to understand their needs</li>
                    <li>Define problems clearly and specifically</li>
                    <li>Ideate creative solutions without constraints</li>
                    <li>Prototype rapidly and test early</li>
                    <li>Iterate based on user feedback</li>
                </ul>
                
                <h3>Impact on Tech Industry</h3>
                <p>Companies adopting design thinking report higher user satisfaction, reduced development costs, and faster time-to-market for successful products.</p>
                
                <p>The methodology's emphasis on collaboration and diverse perspectives is particularly valuable in today's complex technological landscape.</p>
            `
        },
        '4': {
            title: 'Green Technology Trends',
            date: 'Dec 11, 2024',
            image: 'Sustainability Post',
            content: `
                <p>The green technology sector is experiencing rapid growth as businesses and consumers increasingly prioritize environmental sustainability and climate action.</p>
                
                <h3>Renewable Energy Innovations</h3>
                <p>Solar, wind, and battery technologies are becoming more efficient and cost-effective, making clean energy accessible to more people worldwide.</p>
                
                <h3>Emerging Technologies</h3>
                <ul>
                    <li>Carbon capture and storage systems</li>
                    <li>Green hydrogen production and applications</li>
                    <li>Sustainable materials and biodegradable alternatives</li>
                    <li>Smart grid and energy management systems</li>
                </ul>
                
                <h3>Business Opportunities</h3>
                <p>The transition to sustainable technologies is creating new markets and business models, from circular economy platforms to environmental monitoring services.</p>
                
                <p>Investment in green technology continues to grow, driven by both regulatory requirements and consumer demand for environmentally responsible products.</p>
            `
        },
        '5': {
            title: 'Digital Leadership Skills',
            date: 'Dec 9, 2024',
            image: 'Leadership Post',
            content: `
                <p>The digital transformation era demands new leadership competencies that go beyond traditional management skills to embrace technological fluency and adaptive thinking.</p>
                
                <h3>Essential Digital Skills</h3>
                <p>Modern leaders must understand emerging technologies, data analytics, and digital business models to make informed strategic decisions.</p>
                
                <h3>Key Leadership Attributes</h3>
                <ul>
                    <li>Technological literacy and continuous learning</li>
                    <li>Data-driven decision making capabilities</li>
                    <li>Digital communication and collaboration skills</li>
                    <li>Change management and adaptability</li>
                    <li>Cybersecurity awareness and risk management</li>
                </ul>
                
                <h3>Building Digital Teams</h3>
                <p>Successful digital leaders focus on creating diverse, cross-functional teams that can navigate complex technological challenges while maintaining human-centered values.</p>
                
                <p>The ability to balance technological innovation with ethical considerations is becoming increasingly important in digital leadership roles.</p>
            `
        },
        '6': {
            title: 'Innovation Methodologies',
            date: 'Dec 7, 2024',
            image: 'Innovation Post',
            content: `
                <p>Organizations are adopting systematic approaches to innovation, using proven methodologies to foster creativity and turn ideas into successful products and services.</p>
                
                <h3>Popular Innovation Frameworks</h3>
                <p>From lean startup methodology to agile development and design sprints, companies are using structured approaches to reduce risk and accelerate innovation.</p>
                
                <h3>Key Methodologies</h3>
                <ul>
                    <li>Lean Startup and Build-Measure-Learn cycles</li>
                    <li>Design Sprints for rapid prototyping</li>
                    <li>Agile and Scrum for iterative development</li>
                    <li>Open innovation and crowdsourcing</li>
                    <li>Stage-gate processes for project management</li>
                </ul>
                
                <h3>Creating Innovation Culture</h3>
                <p>Successful innovation requires more than just methodology—it demands a culture that encourages experimentation, learns from failure, and rewards creative thinking.</p>
                
                <p>Organizations that master these methodologies while fostering the right cultural environment are better positioned to thrive in competitive markets.</p>
            `
        }
    };
    
    // 左滑动按钮
    prevBtn.addEventListener('click', function() {
        slider.scrollBy({
            left: -cardWidth,
            behavior: 'smooth'
        });
    });
    
    // 右滑动按钮
    nextBtn.addEventListener('click', function() {
        slider.scrollBy({
            left: cardWidth,
            behavior: 'smooth'
        });
    });
    
    // 触摸滑动支持
    let startX = 0;
    let scrollLeft = 0;
    let isDown = false;
    
    slider.addEventListener('mousedown', function(e) {
        isDown = true;
        slider.style.cursor = 'grabbing';
        startX = e.pageX - slider.offsetLeft;
        scrollLeft = slider.scrollLeft;
    });
    
    slider.addEventListener('mouseleave', function() {
        isDown = false;
        slider.style.cursor = 'grab';
    });
    
    slider.addEventListener('mouseup', function() {
        isDown = false;
        slider.style.cursor = 'grab';
    });
    
    slider.addEventListener('mousemove', function(e) {
        if (!isDown) return;
        e.preventDefault();
        const x = e.pageX - slider.offsetLeft;
        const walk = (x - startX) * 2;
        slider.scrollLeft = scrollLeft - walk;
    });
    
    // Read More按钮功能
    readMoreButtons.forEach(button => {
        button.addEventListener('click', function() {
            const postId = this.getAttribute('data-post');
            const postData = postsData[postId];
            
            if (postData) {
                // 更新文章弹窗内容
                document.getElementById('articleModalTitle').textContent = postData.title;
                document.getElementById('articleModalDate').textContent = postData.date;
                const imageHolder=document.getElementById('articleModalImage');
                if(imageHolder){
                    imageHolder.textContent=postData.image;
                }
                document.getElementById('articleModalContent').innerHTML = postData.content;
                
                // 显示弹窗
                showModal('articleModal');
            }
        });
    });
    
    // 设置初始状态
    slider.style.cursor = 'grab';
}

// (Spinning globe interaction code removed)

// 登录注册弹窗相关功能
function initAuthModals() {
    const loginModal = document.getElementById('loginModal');
    const registerModal = document.getElementById('registerModal');
    const signInBtn = document.querySelector('.btn-signin');
    const joinBtn = document.querySelector('.btn-join');
    const closeBtns = document.querySelectorAll('.close');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    
    // 点击Log in按钮打开登录弹窗
    if (signInBtn) {
        signInBtn.addEventListener('click', function() {
            showModal('loginModal');
        });
    }
    
    // 点击Join for free按钮打开注册弹窗
    if (joinBtn) {
        joinBtn.addEventListener('click', function() {
            showModal('registerModal');
        });
    }
    
    // 关闭按钮功能
    closeBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const modalId = this.getAttribute('data-modal');
            hideModal(modalId);
        });
    });
    
    // 点击背景关闭弹窗
    [loginModal, registerModal].forEach(modal => {
        if (modal) {
            modal.addEventListener('click', function(e) {
                if (e.target === this) {
                    hideModal(this.id);
                }
            });
        }
    });
    
    // ESC键关闭弹窗
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            hideModal('loginModal');
            hideModal('registerModal');
        }
    });
    
    // 登录表单处理
    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            handleLogin(this);
        });
    }
    
    // 注册表单处理
    if (registerForm) {
        registerForm.addEventListener('submit', function(e) {
            e.preventDefault();
            handleRegister(this);
        });
    }
}

// 显示弹窗
function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'flex';
        setTimeout(() => {
            modal.classList.add('show');
        }, 10);
        
        // 防止背景滚动
        document.body.style.overflow = 'hidden';
        
        // 聚焦到第一个输入框
        const firstInput = modal.querySelector('input');
        if (firstInput) {
            setTimeout(() => firstInput.focus(), 300);
        }
    }
}

// 隐藏弹窗
function hideModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal && modal.classList.contains('show')) {
        modal.classList.remove('show');
        setTimeout(() => {
            modal.style.display = 'none';
        }, 300);
        
        // 恢复背景滚动
        document.body.style.overflow = '';
        
        // 清空表单
        const form = modal.querySelector('form');
        if (form) {
            form.reset();
            clearFormErrors(form);
        }
    }
}

// 切换到注册弹窗
function switchToRegister() {
    hideModal('loginModal');
    setTimeout(() => {
        showModal('registerModal');
    }, 300);
}

// 切换到登录弹窗
function switchToLogin() {
    hideModal('registerModal');
    setTimeout(() => {
        showModal('loginModal');
    }, 300);
}

// 处理登录
function handleLogin(form) {
    const email = form.email.value;
    const password = form.password.value;
    const remember = form.remember.checked;
    
    // 清除之前的错误
    clearFormErrors(form);
    
    // 验证邮箱
    if (!validateEmail(email)) {
        showFieldError(form.email, '请输入有效的邮箱地址');
        return;
    }
    
    // 验证密码
    if (password.length < 6) {
        showFieldError(form.password, '密码长度至少为6位');
        return;
    }
    
    // 模拟登录过程
    const submitBtn = form.querySelector('.btn-auth');
    const originalText = submitBtn.textContent;
    submitBtn.textContent = '登录中...';
    submitBtn.disabled = true;
    
    setTimeout(() => {
        // 模拟登录成功
        showMessage('登录成功！欢迎回来！', 'success');
        hideModal('loginModal');
        
        // 更新导航栏状态（可以添加用户头像等）
        updateNavAfterLogin(email);
        
        submitBtn.textContent = originalText;
        submitBtn.disabled = false;
    }, 1500);
}

// 处理注册
function handleRegister(form) {
    const name = form.name.value;
    const email = form.email.value;
    const password = form.password.value;
    const confirmPassword = form.confirmPassword.value;
    const terms = form.terms.checked;
    
    // 清除之前的错误
    clearFormErrors(form);
    
    let hasError = false;
    
    // 验证姓名
    if (name.trim().length < 2) {
        showFieldError(form.name, '姓名至少需要2个字符');
        hasError = true;
    }
    
    // 验证邮箱
    if (!validateEmail(email)) {
        showFieldError(form.email, '请输入有效的邮箱地址');
        hasError = true;
    }
    
    // 验证密码
    if (password.length < 6) {
        showFieldError(form.password, '密码长度至少为6位');
        hasError = true;
    }
    
    // 验证确认密码
    if (password !== confirmPassword) {
        showFieldError(form.confirmPassword, '两次输入的密码不一致');
        hasError = true;
    }
    
    // 验证用户协议
    if (!terms) {
        showMessage('请同意用户协议和隐私政策', 'error');
        hasError = true;
    }
    
    if (hasError) return;
    
    // 模拟注册过程
    const submitBtn = form.querySelector('.btn-auth');
    const originalText = submitBtn.textContent;
    submitBtn.textContent = '注册中...';
    submitBtn.disabled = true;
    
    setTimeout(() => {
        // 模拟注册成功
        showMessage('注册成功！欢迎加入YouthMind！', 'success');
        hideModal('registerModal');
        
        // 更新导航栏状态
        updateNavAfterLogin(email);
        
        submitBtn.textContent = originalText;
        submitBtn.disabled = false;
    }, 1500);
}

// 验证邮箱格式
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// 显示字段错误
function showFieldError(field, message) {
    // 移除旧的错误提示
    const existingError = field.parentNode.querySelector('.field-error');
    if (existingError) {
        existingError.remove();
    }
    
    // 添加错误样式
    field.style.borderColor = '#ef4444';
    field.style.backgroundColor = '#fef2f2';
    
    // 创建错误提示
    const errorDiv = document.createElement('div');
    errorDiv.className = 'field-error';
    errorDiv.textContent = message;
    errorDiv.style.cssText = `
        color: #ef4444;
        font-size: 0.8rem;
        margin-top: 5px;
        animation: fadeIn 0.3s ease;
    `;
    
    field.parentNode.appendChild(errorDiv);
    
    // 聚焦到错误字段
    field.focus();
}

// 清除表单错误
function clearFormErrors(form) {
    // 移除所有错误提示
    const errors = form.querySelectorAll('.field-error');
    errors.forEach(error => error.remove());
    
    // 重置字段样式
    const inputs = form.querySelectorAll('input');
    inputs.forEach(input => {
        input.style.borderColor = '';
        input.style.backgroundColor = '';
    });
}

// 登录后更新导航栏
function updateNavAfterLogin(email) {
    const authButtons = document.querySelector('.auth-buttons');
    if (authButtons) {
        authButtons.innerHTML = `
            <div class="user-info">
                <span class="user-email">${email}</span>
                <button class="btn-logout" onclick="handleLogout()">退出</button>
            </div>
        `;
        
        // 添加用户信息样式
        const style = document.createElement('style');
        style.textContent = `
            .user-info {
                display: flex;
                align-items: center;
                gap: 15px;
            }
            
            .user-email {
                color: var(--text-dark);
                font-weight: 500;
                font-size: 0.9rem;
            }
            
            .btn-logout {
                background: transparent;
                border: 1px solid var(--border-gray);
                color: var(--text-gray);
                padding: 6px 12px;
                border-radius: 15px;
                font-size: 0.8rem;
                cursor: pointer;
                transition: all 0.3s ease;
            }
            
            .btn-logout:hover {
                background-color: var(--light-gray);
                color: var(--text-dark);
            }
        `;
        document.head.appendChild(style);
    }
}

// 处理退出登录
function handleLogout() {
    const authButtons = document.querySelector('.auth-buttons');
    if (authButtons) {
                 authButtons.innerHTML = `
             <button class="btn-signin">Log in</button>
             <button class="btn-join">Join for free</button>
         `;
        
        // 重新绑定事件
        setTimeout(() => {
            initAuthModals();
        }, 100);
        
        showMessage('已成功退出登录', 'success');
    }
} 

// ------------------------------
// 静态世界地图初始化
// ------------------------------
function initWorldMap() {
    const mapElement = document.getElementById('map');
    if (!mapElement || typeof L === 'undefined') return;

    const map = L.map('map', {
        center: [20, 0],
        zoom: 2,
        dragging: false,
        scrollWheelZoom: false,
        doubleClickZoom: false,
        boxZoom: false,
        keyboard: false,
        zoomControl: false,
        tap: false
    });

    // 加载 OpenStreetMap 瓦片
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '',
        interactive: false,
        noWrap: true
    }).addTo(map);

    const markerCoords = [
        [25.2048, 55.2708],   // Dubai
        [51.4545, -2.5879],   // Bristol (UK)
        [39.9042, 116.4074],  // Beijing (China)
        [19.5664, 109.9497]   // Hainan (China)
    ];

    // 添加标记并收集到数组中
    const markers = markerCoords.map(coords => (
        L.circleMarker(coords, {
            radius: 10,
            color: '#ff0000',
            weight: 2,
            fillColor: '#ff3b3b',
            fillOpacity: 1
        }).addTo(map)
    ));

    // 调整视图以包含所有标记
    const bounds = L.latLngBounds(markerCoords);
    map.fitBounds(bounds.pad(0.3));

    // 禁用拖动平移后保持视图固定
    map.once('moveend', () => {
        map.dragging.disable();
    });

} 