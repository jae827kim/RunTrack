"""
Google Gemini API를 활용한 AI 신발 추천 서비스
"""
import google.generativeai as genai
from app.config import settings
import json

class ShoeRecommendationService:
    """AI 신발 추천 서비스"""
    
    def __init__(self):
        """Gemini API 초기화"""
        genai.configure(api_key=settings.GEMINI_API_KEY)
        self.model = genai.GenerativeModel("gemini-pro")
    
    async def get_shoe_recommendations(
        self,
        weight_kg: float,
        height_cm: float,
        foot_size: str,
        foot_width: str,  # narrow, regular, wide
        arch_type: str,  # low, neutral, high
        running_style: str,  # cushioning, speed, trail, all-purpose
        budget_won: int,
        preferred_brands: list = None
    ) -> dict:
        """
        사용자의 신체 정보와 선호도를 바탕으로 AI가 신발을 추천
        
        Args:
            weight_kg: 체중 (kg)
            height_cm: 키 (cm)
            foot_size: 발 크기 (예: "280mm", "US 10")
            foot_width: 발볼 너비
            arch_type: 아치 타입
            running_style: 러닝 스타일
            budget_won: 예산 (원)
            preferred_brands: 선호 브랜드 리스트
        
        Returns:
            추천 신발 정보 리스트
        """
        
        prompt = self._build_prompt(
            weight_kg, height_cm, foot_size, foot_width,
            arch_type, running_style, budget_won, preferred_brands
        )
        
        try:
            response = self.model.generate_content(prompt)
            recommendations = self._parse_response(response.text)
            return recommendations
        except Exception as e:
            return {
                "error": str(e),
                "message": "AI 추천 생성 중 오류가 발생했습니다"
            }
    
    def _build_prompt(
        self, weight_kg, height_cm, foot_size, foot_width,
        arch_type, running_style, budget_won, preferred_brands
    ) -> str:
        """Gemini API에 전달할 프롬프트 생성"""
        
        brands_str = ", ".join(preferred_brands) if preferred_brands else "제약 없음"
        
        prompt = f"""
당신은 전문 러닝화 상담사입니다. 다음 사용자 정보를 바탕으로 3개의 최적화된 러닝화를 추천해주세요.

사용자 정보:
- 체중: {weight_kg}kg
- 키: {height_cm}cm
- 발 크기: {foot_size}
- 발볼 너비: {foot_width}
- 아치 타입: {arch_type}
- 러닝 스타일: {running_style}
- 예산: {budget_won}원
- 선호 브랜드: {brands_str}

각 추천 신발에 대해 JSON 형식으로 다음 정보를 제공하세요:
{{
  "rank": 1,
  "name": "신발명",
  "brand": "브랜드",
  "model": "모델명",
  "approximate_price_won": 추정가격,
  "why_recommended": "추천 이유",
  "pros": ["장점1", "장점2", "장점3"],
  "cons": ["단점1", "단점2"],
  "durability_km": 예상내구성,
  "suitable_for": ["용도1", "용도2"]
}}

응답은 JSON 배열 형식으로 제공하세요. 예:
[{{"rank": 1, ...}}, {{"rank": 2, ...}}, {{"rank": 3, ...}}]

한국어로 응답해주세요.
"""
        return prompt
    
    def _parse_response(self, response_text: str) -> dict:
        """Gemini API 응답 파싱"""
        try:
            # JSON 데이터 추출
            import re
            json_match = re.search(r'\[.*\]', response_text, re.DOTALL)
            if json_match:
                recommendations = json.loads(json_match.group())
                return {
                    "success": True,
                    "recommendations": recommendations,
                    "count": len(recommendations)
                }
            else:
                return {
                    "success": False,
                    "message": "응답 형식이 올바르지 않습니다",
                    "raw_response": response_text
                }
        except json.JSONDecodeError as e:
            return {
                "success": False,
                "message": f"JSON 파싱 오류: {str(e)}",
                "raw_response": response_text
            }

# 서비스 인스턴스
shoe_recommendation_service = ShoeRecommendationService()
